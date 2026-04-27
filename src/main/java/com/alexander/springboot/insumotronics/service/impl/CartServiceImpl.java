package com.alexander.springboot.insumotronics.service.impl;

import com.alexander.springboot.insumotronics.model.CartM;
import com.alexander.springboot.insumotronics.model.ItemCartM;
import com.alexander.springboot.insumotronics.entity.Cart;
import com.alexander.springboot.insumotronics.entity.ItemCart;
import com.alexander.springboot.insumotronics.entity.MyUser;
import com.alexander.springboot.insumotronics.model.CreateCartM;
import com.alexander.springboot.insumotronics.repository.CartRepository;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import com.alexander.springboot.insumotronics.service.CartService;
import com.alexander.springboot.insumotronics.exception.CartNotFoundException;
import com.alexander.springboot.insumotronics.exception.MyUserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository repository;
    private final MyUserRepository userRepository;

    public CartServiceImpl(CartRepository repository, MyUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<CartM> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(cart -> {
            recalculateTotalPrice(cart);
            return convertToDTO(cart);
        });
    }

    @Override
    public Page<CartM> findByUserId(UUID userId, Pageable pageable) {
        return repository.findByMyUserId(userId, pageable).map(cart -> {
            recalculateTotalPrice(cart);
            return convertToDTO(cart);
        });
    }

    @Override
    public CartM getOrCreateByUserId(UUID userId) {
        Cart cart = repository.findTopByMyUserIdOrderByCreationDateDesc(userId)
                .orElseGet(() -> {
                    MyUser user = userRepository.findById(userId)
                            .orElseThrow(() -> new MyUserNotFoundException("User with ID: " + userId + ", not found."));
                    Cart newCart = new Cart();
                    newCart.setMyUser(user);
                    newCart.setItems(new ArrayList<>());
                    newCart.setTotalPrice(0.0f);
                    return repository.save(newCart);
                });
        recalculateTotalPrice(cart);
        return convertToDTO(cart);
    }

    @Override
    public CartM create(CreateCartM cartM) {
        MyUser user = userRepository.findById(cartM.myUserId())
                .orElseThrow(() -> new MyUserNotFoundException("User with ID: " + cartM.myUserId() + ", not found."));

        Cart cart = new Cart();
        cart.setMyUser(user);
        cart.setItems(new ArrayList<>());
        cart.setTotalPrice(0.0f);
        Cart savedCart = repository.save(cart);
        recalculateTotalPrice(savedCart);
        return convertToDTO(savedCart);
    }

    @Override
    public java.util.Optional<CartM> findById(UUID id) {
        return repository.findById(id).map(cart -> {
            recalculateTotalPrice(cart);
            return convertToDTO(cart);
        });
    }

    @Override
    public void delete(UUID id) {
        Cart cart = repository.findById(id)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + id + ", not found."));
        repository.delete(cart);
    }

    private void recalculateTotalPrice(Cart cart) {
        float total = 0.0f;
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            for (ItemCart item : cart.getItems()) {
                if (item.getProduct() != null) {
                    total += item.getProduct().getPrice() * item.getQuantity();
                }
            }
        }
        cart.setTotalPrice(total);
        repository.save(cart);
    }

    private CartM convertToDTO(Cart cart) {
        return new CartM(
                cart.getId(),
                cart.getMyUser().getId(),
                cart.getItems() == null ? new ArrayList<>() : cart.getItems().stream().map(this::convertItemToDTO).toList(),
                cart.getTotalPrice(),
                cart.getCreationDate(),
                cart.getUpdateDate()
        );
    }

    private ItemCartM convertItemToDTO(com.alexander.springboot.insumotronics.entity.ItemCart itemCart) {
        return new ItemCartM(
                itemCart.getId(),
                itemCart.getQuantity(),
                itemCart.getCart().getId(),
                itemCart.getProduct().getId(),
                itemCart.getProduct().getName(),
                itemCart.getProduct().getPrice(),
                itemCart.getCreationDate(),
                itemCart.getUpdateDate()
        );
    }
}

