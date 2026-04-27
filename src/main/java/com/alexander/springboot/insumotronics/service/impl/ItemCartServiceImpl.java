package com.alexander.springboot.insumotronics.service.impl;

import com.alexander.springboot.insumotronics.model.ItemCartM;
import com.alexander.springboot.insumotronics.entity.Cart;
import com.alexander.springboot.insumotronics.entity.ItemCart;
import com.alexander.springboot.insumotronics.entity.MyUser;
import com.alexander.springboot.insumotronics.entity.Product;
import com.alexander.springboot.insumotronics.model.CreateItemCartM;
import java.util.UUID;
import com.alexander.springboot.insumotronics.repository.CartRepository;
import com.alexander.springboot.insumotronics.repository.ItemCartRepository;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import com.alexander.springboot.insumotronics.repository.ProductRepository;
import com.alexander.springboot.insumotronics.service.ItemCartService;
import com.alexander.springboot.insumotronics.exception.CartNotFoundException;
import com.alexander.springboot.insumotronics.exception.ItemCartNotFoundException;
import com.alexander.springboot.insumotronics.exception.MyUserNotFoundException;
import com.alexander.springboot.insumotronics.exception.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItemCartServiceImpl implements ItemCartService {

    private final ItemCartRepository repository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final MyUserRepository userRepository;

    public ItemCartServiceImpl(ItemCartRepository repository,
                               CartRepository cartRepository,
                               ProductRepository productRepository,
                               MyUserRepository userRepository) {
        this.repository = repository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<ItemCartM> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public Page<ItemCartM> findByCartId(java.util.UUID cartId, Pageable pageable) {
        return repository.findByCartId(cartId, pageable).map(this::convertToDTO);
    }

    @Override
    public java.util.Optional<ItemCartM> findById(java.util.UUID id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    @Override
    public ItemCartM create(CreateItemCartM itemCartM) {
        if (itemCartM.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Cart cart = cartRepository.findById(itemCartM.cartId())
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + itemCartM.cartId() + ", not found."));

        Product product = productRepository.findById(itemCartM.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + itemCartM.productId() + ", not found."));

        var existingItem = repository.findByCartIdAndProductId(cart.getId(), product.getId());
        if (existingItem.isPresent()) {
            ItemCart itemCart = existingItem.get();
            itemCart.setQuantity(itemCart.getQuantity() + itemCartM.quantity());
            ItemCart saved = repository.save(itemCart);

            Cart refreshedCart = cartRepository.findById(cart.getId())
                    .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
            recalculateCartTotal(refreshedCart);
            return convertToDTO(saved);
        }

        ItemCart itemCart = new ItemCart();
        itemCart.setCart(cart);
        itemCart.setProduct(product);
        itemCart.setQuantity(itemCartM.quantity());
        ItemCart saved = repository.save(itemCart);
        // Force refresh of cart to get updated items
        Cart refreshedCart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
        recalculateCartTotal(refreshedCart);
        return convertToDTO(saved);
    }

    @Override
    public ItemCartM addToCart(UUID userId, UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + productId + ", not found."));

        var existingItem = repository.findByCartIdAndProductId(cart.getId(), product.getId());
        if (existingItem.isPresent()) {
            ItemCart itemCart = existingItem.get();
            itemCart.setQuantity(itemCart.getQuantity() + quantity);
            ItemCart saved = repository.save(itemCart);

            Cart refreshedCart = cartRepository.findById(cart.getId())
                    .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
            recalculateCartTotal(refreshedCart);
            return convertToDTO(saved);
        }

        ItemCart itemCart = new ItemCart();
        itemCart.setCart(cart);
        itemCart.setProduct(product);
        itemCart.setQuantity(quantity);
        ItemCart saved = repository.save(itemCart);

        Cart refreshedCart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
        recalculateCartTotal(refreshedCart);
        return convertToDTO(saved);
    }

    private Cart getOrCreateCart(UUID userId) {
        Cart cart = cartRepository.findTopByMyUserIdOrderByCreationDateDesc(userId)
                .orElse(null);
        
        if (cart == null) {
            MyUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new MyUserNotFoundException("User with ID: " + userId + ", not found."));
            cart = new Cart();
            cart.setMyUser(user);
            cart.setItems(new java.util.ArrayList<>());
            cart.setTotalPrice(0.0f);
            cart = cartRepository.save(cart);
        }
        
        return cart;
    }

    @Override
    public ItemCartM updateQuantity(java.util.UUID id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        ItemCart itemCart = repository.findById(id)
                .orElseThrow(() -> new ItemCartNotFoundException("ItemCart with ID: " + id + ", not found."));
        itemCart.setQuantity(quantity);
        ItemCart saved = repository.save(itemCart);
        recalculateCartTotal(itemCart.getCart());
        return convertToDTO(saved);
    }

    @Override
    public void delete(java.util.UUID id) {
        ItemCart itemCart = repository.findById(id)
                .orElseThrow(() -> new ItemCartNotFoundException("ItemCart with ID: " + id + ", not found."));
        Cart cart = itemCart.getCart();
        repository.delete(itemCart);

        Cart refreshedCart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
        recalculateCartTotal(refreshedCart);
    }

    private void recalculateCartTotal(Cart cart) {

        Cart refreshedCart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + cart.getId() + ", not found."));
        
        float total = 0.0f;
        if (refreshedCart.getItems() != null && !refreshedCart.getItems().isEmpty()) {
            for (ItemCart item : refreshedCart.getItems()) {
                if (item.getProduct() != null) {
                    total += item.getProduct().getPrice() * item.getQuantity();
                }
            }
        }
        refreshedCart.setTotalPrice(total);
        cartRepository.save(refreshedCart);
    }

    private ItemCartM convertToDTO(ItemCart itemCart) {
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

