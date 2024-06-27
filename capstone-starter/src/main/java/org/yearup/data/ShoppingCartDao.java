package org.yearup.data;

import org.yearup.models.ShoppingCart;

public interface ShoppingCartDao {
    ShoppingCart getByUserId(int userId);

    void updateProductInCart(int productId, int userId, int quantity);

    void clearCart(int userId);
    void addProductToCart(int productId, int userId);
}