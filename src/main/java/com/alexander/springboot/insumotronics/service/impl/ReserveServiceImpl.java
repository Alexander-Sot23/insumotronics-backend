package com.alexander.springboot.insumotronics.service.impl;

import com.alexander.springboot.insumotronics.model.ItemReserveM;
import com.alexander.springboot.insumotronics.model.ReserveM;
import com.alexander.springboot.insumotronics.entity.Cart;
import com.alexander.springboot.insumotronics.entity.ItemCart;
import com.alexander.springboot.insumotronics.entity.ItemReserve;
import com.alexander.springboot.insumotronics.entity.Product;
import com.alexander.springboot.insumotronics.entity.Reserve;
import com.alexander.springboot.insumotronics.enums.ReserveStatus;
import com.alexander.springboot.insumotronics.exception.CartNotFoundException;
import com.alexander.springboot.insumotronics.exception.ProductNotFoundException;
import com.alexander.springboot.insumotronics.exception.ReserveNotFoundException;
import com.alexander.springboot.insumotronics.repository.CartRepository;
import com.alexander.springboot.insumotronics.repository.ReserveRepository;
import com.alexander.springboot.insumotronics.service.email.EmailService;
import com.alexander.springboot.insumotronics.service.ReserveService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReserveServiceImpl implements ReserveService {

    private final ReserveRepository reserveRepository;
    private final CartRepository cartRepository;
    private final EmailService emailService;

    @Autowired
    public ReserveServiceImpl(ReserveRepository reserveRepository,
                              CartRepository cartRepository,
                              EmailService emailService) {
        this.reserveRepository = reserveRepository;
        this.cartRepository = cartRepository;
        this.emailService = emailService;
    }

    @Override
    public long countTotalReservations() {
        return reserveRepository.count();
    }

    @Override
    public long countByUserId(UUID userId) {
        return reserveRepository.countByMyUserId(userId);
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, ReserveStatus status) {
        return reserveRepository.countByMyUserIdAndStatus(userId, status);
    }

    @Override
    public List<ReserveM> findActiveReserves(UUID userId) {
        List<ReserveStatus> activeStatuses = List.of(ReserveStatus.SUPERVISION, ReserveStatus.CONFIRMADA);

        return reserveRepository.findByMyUserIdAndStatusIn(userId, activeStatuses)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public Page<ReserveM> findAll(Pageable pageable) {
        return reserveRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public Page<ReserveM> findByStatus(String status, Pageable pageable) {
        return reserveRepository.findByStatus(ReserveStatus.valueOf(status.toUpperCase()), pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ReserveM> findByUserId(UUID userId, Pageable pageable) {
        return reserveRepository.findByMyUserId(userId, pageable).map(this::convertToDTO);
    }

    @Override
    public Page<ReserveM> findByUserIdAndStatus(UUID userId, String status, Pageable pageable) {
        return reserveRepository.findByMyUserIdAndStatus(userId, ReserveStatus.valueOf(status.toUpperCase()), pageable)
                .map(this::convertToDTO);
    }

    @Override
    public ReserveM findById(UUID id) {
        return reserveRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ReserveNotFoundException("Reserve with ID: " + id + ", not found."));
    }

    @Override
    public ReserveM checkoutCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cartId + ", not found."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item to checkout.");
        }

        Reserve reserve = new Reserve();
        reserve.setStatus(ReserveStatus.SUPERVISION);
        reserve.setTotal(cart.getTotalPrice());
        reserve.setMyUser(cart.getMyUser());

        ArrayList<ItemReserve> reserveItems = new ArrayList<>();
        for (ItemCart itemCart : cart.getItems()) {
            Product product = itemCart.getProduct();
            if (product == null) {
                throw new ProductNotFoundException("Product for item " + itemCart.getId() + " not found.");
            }
            ItemReserve itemReserve = new ItemReserve();
            itemReserve.setReserve(reserve);
            itemReserve.setProduct(product);
            itemReserve.setQuantity(itemCart.getQuantity());
            itemReserve.setPriceU(product.getPrice());
            itemReserve.setTotal(product.getPrice() * itemCart.getQuantity());
            reserveItems.add(itemReserve);
        }

        reserve.setItems(reserveItems);
        Reserve savedReserve = reserveRepository.save(reserve);

        cart.getItems().clear();
        cart.setTotalPrice(0.0f);
        cartRepository.save(cart);

        return convertToDTO(savedReserve);
    }

    @Override
    public ReserveM confirmReserve(UUID id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ReserveNotFoundException("Reserve with ID: " + id + ", not found."));

        if (reserve.getStatus() != ReserveStatus.SUPERVISION) {
            throw new IllegalStateException("Reserve with ID: " + id + " cannot be confirmed in status " + reserve.getStatus());
        }

        reserve.setStatus(ReserveStatus.CONFIRMADA);
        Reserve saved = reserveRepository.save(reserve);

        try {
            emailService.sendConfirmPurchase(
                    reserve.getMyUser().getEmail(),
                    reserve.getMyUser().getName(),
                    "Su pedido ha sido confirmado. Total a pagar: " + reserve.getTotal());
        } catch (MessagingException ex) {
            throw new RuntimeException("Error sending confirmation email", ex);
        }

        return convertToDTO(saved);
    }

    @Override
    public ReserveM cancelReserve(UUID id, String message) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ReserveNotFoundException("Reserve with ID: " + id + ", not found."));

        if (reserve.getStatus() != ReserveStatus.SUPERVISION) {
            throw new IllegalStateException("Reserve with ID: " + id + " cannot be canceled in status " + reserve.getStatus());
        }
        reserve.setStatus(ReserveStatus.CANCELADA);
        Reserve saved = reserveRepository.save(reserve);

        try {
            emailService.sendCancelPurchase(
                    reserve.getMyUser().getEmail(),
                    reserve.getMyUser().getName(),
                    message);
        } catch (MessagingException ex) {
            throw new RuntimeException("Error sending cancellation email", ex);
        }

        return convertToDTO(saved);
    }

    private ReserveM convertToDTO(Reserve reserve) {
        return new ReserveM(
                reserve.getId(),
                reserve.getStatus(),
                reserve.getTotal(),
                reserve.getMyUser().getId(),
                reserve.getItems() == null ? new ArrayList<>() : reserve.getItems().stream().map(this::convertItemToDTO).toList(),
                reserve.getCreationDate(),
                reserve.getUpdateDate()
        );
    }

    private ItemReserveM convertItemToDTO(ItemReserve itemReserve) {
        return new ItemReserveM(
                itemReserve.getId(),
                itemReserve.getQuantity(),
                itemReserve.getPriceU(),
                itemReserve.getTotal(),
                itemReserve.getReserve().getId(),
                itemReserve.getProduct().getId(),
                itemReserve.getProduct().getName(),
                itemReserve.getCreationDate(),
                itemReserve.getUpdateDate()
        );
    }
}
