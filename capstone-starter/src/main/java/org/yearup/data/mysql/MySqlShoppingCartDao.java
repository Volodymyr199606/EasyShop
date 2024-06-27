package org.yearup.data.mysql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;





@Component
public class MySqlShoppingCartDao extends  MySqlDaoBase implements ShoppingCartDao {


    private final ProductDao productDao;

    private static final Logger logger = LoggerFactory.getLogger(MySqlShoppingCartDao.class);

    @Autowired
    public MySqlShoppingCartDao(DataSource dataSource, ProductDao productDao)
    {
        super(dataSource);
        this.productDao = productDao;
    }

    @Override
    public ShoppingCart getByUserId(int userId)
    {
        ShoppingCart shoppingCart = new ShoppingCart();

        try(Connection connection = getConnection()) {
            String sql = "SELECT * FROM shopping_cart WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet row = ps.executeQuery();
            while (row.next()){
                int productId = row.getInt("product_id");
                int quantity = row.getInt("quantity");

                Product product = productDao.getById(productId);
                ShoppingCartItem item = new ShoppingCartItem();
                item.setProduct(product);
                item.setQuantity(quantity);

                shoppingCart.add(item);
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return shoppingCart;
    }

    @Override
    public void updateProductInCart(int productId, int userId, int quantity)
    {
        String sql = """
                UPDATE shopping_cart
                SET quantity = ?
                WHERE user_id = ? AND product_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, productId);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearCart(int userId)
    {
        logger.info("Clearing cart for user with ID: {}", userId);
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Number of rows deleted: {}", rowsAffected);
        } catch (SQLException e) {
            logger.error("Error while clearing cart for user with ID: {}", userId, e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void addProductToCart(int productId, int userId) {
        ShoppingCartItem currentItem = getCartItem(productId, userId);

        if(currentItem == null) {
            String query = "INSERT INTO  shopping_cart (user_id, product_id) VALUES (?, ?)";
            try (Connection connection = getConnection();
                 PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, productId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        else {
            updateProductInCart(productId, userId, currentItem.getQuantity() + 1);
        }
    }


    public ShoppingCartItem getCartItem(int productId, int userId) {
        ShoppingCartItem item = null;
        try (Connection connection = getConnection()) {
            String sql = "SELECT * FROM shopping_cart WHERE user_id = ? AND product_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ResultSet row = ps.executeQuery();
            if (row.next()) {
                item = new ShoppingCartItem();
                int quantity = row.getInt("quantity");
                Product product = productDao.getById(productId);
                item.setProduct(product);
                item.setQuantity(quantity);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return item;
    }


}
