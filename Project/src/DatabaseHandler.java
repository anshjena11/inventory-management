import java.sql.*;
import java.util.Scanner;

public class DatabaseHandler {
    // Database connection details
    private static final String URL = "jdbc:mysql://localhost:3306/inventory_manager";
    private static final String USER = "root";
    private static final String PASSWORD = "ansh2004";

    // Method to establish connection
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            System.out.println("Database connection failed! Error: " + e.getMessage());
        }
        return conn;
    }

    // Method to add a product (updates stock if product exists)
    public static void addProduct(String name, double price, int stock) {
        String checkSql = "SELECT stock FROM products WHERE name = ?";
        String updateSql = "UPDATE products SET stock = stock + ? WHERE name = ?";
        String insertSql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            if (conn == null) {
                System.out.println("Connection failed. Cannot add/update product.");
                return;
            }

            // Check if product already exists
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Product exists -> Update stock
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, stock);
                    updateStmt.setString(2, name);
                    int rowsUpdated = updateStmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        System.out.println("Stock updated successfully for " + name + "!");
                    }
                }
            } else {
                // Product does not exist -> Insert new product
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, name);
                    insertStmt.setDouble(2, price);
                    insertStmt.setInt(3, stock);
                    int rowsInserted = insertStmt.executeUpdate();

                    if (rowsInserted > 0) {
                        System.out.println("Product added successfully!");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error adding/updating product! " + e.getMessage());
        }
    }

    // Method to display products
    public static void displayProducts() {
        String sql = "SELECT * FROM products";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (conn == null) return;

            System.out.println("ID | Name | Price | Stock");
            while (rs.next()) {
                System.out.println(rs.getInt("product_id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getDouble("price") + " | " +
                        rs.getInt("stock"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching products.");
        }
    }

    // Method to sell a product (Handles inventory shortages)
    public static void sellProduct(int productId, int quantity) {
        String checkStockSQL = "SELECT stock FROM products WHERE product_id = ?";
        String updateStockSQL = "UPDATE products SET stock = stock - ? WHERE product_id = ?";
        String insertSaleSQL = "INSERT INTO sales (product_id, quantity) VALUES (?, ?)";

        try (Connection conn = connect();
             PreparedStatement checkStockStmt = conn.prepareStatement(checkStockSQL);
             PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSQL);
             PreparedStatement insertSaleStmt = conn.prepareStatement(insertSaleSQL)) {

            if (conn == null) {
                System.out.println("Connection failed. Cannot process sale.");
                return;
            }

            // Step 1: Check available stock
            checkStockStmt.setInt(1, productId);
            ResultSet rs = checkStockStmt.executeQuery();

            if (rs.next()) {
                int availableStock = rs.getInt("stock");

                if (availableStock >= quantity) {
                    // Step 2: Update stock
                    updateStockStmt.setInt(1, quantity);
                    updateStockStmt.setInt(2, productId);
                    updateStockStmt.executeUpdate();

                    // Step 3: Record sale in 'sales' table
                    insertSaleStmt.setInt(1, productId);
                    insertSaleStmt.setInt(2, quantity);
                    insertSaleStmt.executeUpdate();

                    System.out.println("Sale successful! Remaining stock: " + (availableStock - quantity));
                } else {
                    System.out.println("Not enough stock available! Only " + availableStock + " left.");
                }
            } else {
                System.out.println("Product ID " + productId + " not found.");
            }

        } catch (SQLException e) {
            System.out.println("Sale failed! " + e.getMessage());
        }
    }

    // **NEW** Method to view sales
    public static void viewSales() {
        String sql = "SELECT s.sale_id, p.name, s.quantity, s.sale_date " +
                "FROM sales s " +
                "JOIN products p ON s.product_id = p.product_id " +
                "ORDER BY s.sale_date DESC";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (conn == null) return;

            System.out.println("\nSales History:");
            System.out.println("Sale ID | Product Name | Quantity | Date");
            while (rs.next()) {
                System.out.println(rs.getInt("sale_id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getInt("quantity") + " | " +
                        rs.getTimestamp("sale_date"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching sales data.");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Add Product");
            System.out.println("2. View Products");
            System.out.println("3. Sell a Product");
            System.out.println("4. View Sales");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            if (choice == 1) {
                System.out.print("Enter product name: ");
                String name = scanner.nextLine();

                System.out.print("Enter product price: ");
                double price = scanner.nextDouble();

                System.out.print("Enter stock quantity: ");
                int stock = scanner.nextInt();

                addProduct(name, price, stock);
            } else if (choice == 2) {
                displayProducts();
            } else if (choice == 3) {
                System.out.print("Enter product ID to sell: ");
                int productId = scanner.nextInt();
                System.out.print("Enter quantity to sell: ");
                int quantity = scanner.nextInt();
                sellProduct(productId, quantity);
            } else if (choice == 4) {
                viewSales();
            } else if (choice == 5) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
        scanner.close();
    }
}
