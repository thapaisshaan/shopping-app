import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ============================================================
//  INVENTORY MANAGEMENT SYSTEM - GAMEHUB
//  Compatible with Java 8 and above
//  By: Mahmoud Mohamed & Ishaan Thapa
// ============================================================

enum TransactionType { ADD, REMOVE, RESTOCK, PURCHASE, PRICE_UPDATE }

// ── Product ──────────────────────────────────────────────────
class Product {
    private String productId;
    private String name;
    private double price;
    private int quantity;

    public Product(String productId, String name, double price, int quantity) {
        this.productId = productId;
        this.name      = name;
        this.price     = price;
        this.quantity  = quantity;
    }

    public String getProductId() { return productId; }
    public String getName()      { return name; }
    public double getPrice()     { return price; }
    public int    getQuantity()  { return quantity; }

    public void updatePrice(double newPrice) {
        if (newPrice < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.price = newPrice;
    }

    public void adjustQuantity(int amount) {
        if (this.quantity + amount < 0)
            throw new IllegalArgumentException("Not enough stock for: " + name);
        this.quantity += amount;
    }

    public String getDetails() {
        return String.format("[%s] %-25s | $%8.2f | Stock: %d", productId, name, price, quantity);
    }

    // Use '|' as delimiter — commas in product names would break CSV parsing
    public String toCsv() {
        return productId + "|" + name + "|" + price + "|" + quantity;
    }

    public static Product fromCsv(String line) {
        String[] p = line.split("\\|", 4);
        if (p.length < 4) throw new IllegalArgumentException("Malformed product record: " + line);
        return new Product(p[0], p[1], Double.parseDouble(p[2]), Integer.parseInt(p[3]));
    }
}

// ── Transaction ───────────────────────────────────────────────
class Transaction {
    private final String    transactionId;
    private TransactionType type;
    private LocalDateTime   timestamp;
    private String          productId;
    private int             quantityChanged;
    private String          performedBy;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Transaction(TransactionType type, String productId, int qty, String user) {
        this.transactionId   = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type            = type;
        this.timestamp       = LocalDateTime.now();
        this.productId       = productId;
        this.quantityChanged = qty;
        this.performedBy     = user;
    }

    public Transaction(String id, TransactionType type, LocalDateTime ts,
                       String productId, int qty, String user) {
        this.transactionId   = id;
        this.type            = type;
        this.timestamp       = ts;
        this.productId       = productId;
        this.quantityChanged = qty;
        this.performedBy     = user;
    }

    // Use '|' as delimiter — usernames with commas would break CSV parsing
    public String toCsv() {
        return transactionId + "|" + type + "|" + timestamp.format(FMT)
                + "|" + productId + "|" + quantityChanged + "|" + performedBy;
    }

    public static Transaction fromCsv(String line) {
        String[] p = line.split("\\|", 6);
        if (p.length < 6) throw new IllegalArgumentException("Malformed transaction record: " + line);
        return new Transaction(
                p[0],
                TransactionType.valueOf(p[1]),
                LocalDateTime.parse(p[2], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                p[3],
                Integer.parseInt(p[4]),
                p[5]
        );
    }

    @Override
    public String toString() {
        return String.format("TXN#%s | %-12s | Product: %-6s | Qty: %+4d | By: %-12s | %s",
                transactionId, type, productId,
                quantityChanged, performedBy, timestamp.format(FMT));
    }

    public TransactionType getType() {
        return type;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(int quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public static DateTimeFormatter getFmt() {
        return FMT;
    }
}

// ── DatabaseManager ───────────────────────────────────────────
class DatabaseManager {
    private static final String DATA_DIR      = "data/";
    private static final String PRODUCTS_FILE = DATA_DIR + "products.csv";
    private static final String TXN_FILE      = DATA_DIR + "transactions.csv";

    public DatabaseManager() {
        new File(DATA_DIR).mkdirs();
    }

    public void saveProducts(Map<String, Product> products) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("productId|name|price|quantity");
            for (Product p : products.values()) lines.add(p.toCsv());
            Files.write(Paths.get(PRODUCTS_FILE), lines);
        } catch (IOException e) {
            System.out.println("Error saving products.");
        }
    }

    public void saveTransactions(List<Transaction> transactions) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("transactionId|type|timestamp|productId|quantityChanged|performedBy");
            for (Transaction t : transactions) lines.add(t.toCsv());
            Files.write(Paths.get(TXN_FILE), lines);
        } catch (IOException e) {
            System.out.println("Error saving transactions.");
        }
    }

    public Map<String, Product> loadProducts() {
        Map<String, Product> products = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(PRODUCTS_FILE));
            int skipped = 0;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                try {
                    Product p = Product.fromCsv(lines.get(i));
                    products.put(p.getProductId(), p);
                } catch (Exception e) {
                    skipped++;
                }
            }
            if (skipped > 0)
                System.out.println("    [!] " + skipped + " corrupted product record(s) skipped.");
        } catch (IOException e) {
            System.out.println("Starting fresh inventory...");
        }
        return products;
    }

    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(TXN_FILE));
            int skipped = 0;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                try {
                    transactions.add(Transaction.fromCsv(lines.get(i)));
                } catch (Exception e) {
                    skipped++;
                }
            }
            if (skipped > 0)
                System.out.println("    [!] " + skipped + " corrupted transaction record(s) skipped.");
        } catch (IOException e) {
            // No saved transactions yet — that's fine
        }
        return transactions;
    }
}

// ── Main (shoppingapp) ────────────────────────────────────────
public class shoppingapp {
    private static final String GOLD  = "[33m";
    private static final String CYAN  = "[36m";
    private static final String GREEN = "[32m";
    private static final String RED   = "[31m";
    private static final String RESET = "[0m";

    private static final List<Product>        cart         = new ArrayList<>();
    private static List<Transaction>    transactions = new ArrayList<>();
    private static DatabaseManager      db;
    private static Map<String, Product> inventory;
    private static String               username;
    private static final Scanner              sc           = new Scanner(System.in);

    public static void main(String[] args) {
        db           = new DatabaseManager();
        inventory    = db.loadProducts();
        transactions = db.loadTransactions();

        // Pre-load GameHub inventory if empty
        if (inventory.isEmpty()) {
            String[][] items = {
                {"P501", "PlayStation 5 Console",   "499.99", "10"},
                {"P502", "DualSense Controller",     "69.99",  "25"},
                {"P503", "PS5 Pulse 3D Headset",     "99.99",  "15"},
                {"G701", "RTX 4080 Gaming PC",       "2499.00", "5"},
                {"G702", "Mechanical RGB Keyboard",  "129.50", "20"},
                {"G703", "Ultra-Wide 4K Monitor",    "850.00",  "8"},
                {"G704", "Pro Gaming Mouse",          "79.99",  "30"},
                {"S801", "Spider-Man 2 (PS5)",        "69.99",  "50"},
                {"S802", "Elden Ring (PC)",           "59.99",  "40"}
            };
            for (String[] i : items)
                inventory.put(i[0], new Product(i[0], i[1],
                        Double.parseDouble(i[2]), Integer.parseInt(i[3])));
            db.saveProducts(inventory);
        }

        clearScreen();
        drawCentered("WELCOME TO GAMEHUB");

        // Validate username is not empty
        username = "";
        while (username.isEmpty()) {
            System.out.print("\n    Please Enter Your Username: ");
            username = sc.nextLine().trim();
            if (username.isEmpty()) System.out.println("    [!] Username cannot be empty.");
        }

        // Choose mode
        clearScreen();
        drawCentered("GAMEHUB - SELECT MODE");
        System.out.println("\n    " + CYAN + "[1]" + RESET + " Customer  - Browse & Shop");
        System.out.println("    " + CYAN + "[2]" + RESET + " Admin     - Manage Inventory");
        System.out.print("\n    Choice: ");
        String mode = sc.nextLine().trim();

        if (mode.equals("2")) {
            adminMenu();
        } else {
            shoppingLoop();
        }
    }

    // ── CUSTOMER SHOPPING LOOP ────────────────────────────────
    private static void shoppingLoop() {
        boolean shopping = true;
        while (shopping) {
            clearScreen();
            drawCentered("GAMEHUB'S PRODUCT CATALOG");
            System.out.println(CYAN + "    ------------------------------------------------------------" + RESET);
            List<Product> products = new ArrayList<>(inventory.values());
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                System.out.printf("    [%d] %-30s | $%8.2f | Stock: %d%n",
                        (i + 1), p.getName(), p.getPrice(), p.getQuantity());
            }
            System.out.println(CYAN + "    ------------------------------------------------------------" + RESET);
            drawCentered("[C] View Cart (" + cart.size() + " items) | [S] Checkout | [X] Exit");
            System.out.print("\n    Select an item number to add to cart: ");
            String input = sc.nextLine().trim().toUpperCase();

            switch (input) {
                case "S" -> {
                    checkout();
                    shopping = false;
                }
                case "X" -> shopping = false;
                case "C" -> viewCart();
                default -> {
                    try {
                        int idx = Integer.parseInt(input) - 1;
                        Product selected = products.get(idx);
                        // Count how many of this item are already in the cart
                        long inCart = 0;
                        for (Product c : cart)
                            if (c.getProductId().equals(selected.getProductId())) inCart++;
                        if (selected.getQuantity() - inCart > 0) {
                            cart.add(selected);
                            System.out.println(GOLD + "    Added " + selected.getName() + " to cart!" + RESET);
                        } else {
                            System.out.println(RED + "    Out of stock!" + RESET);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("    Invalid choice.");
                    }
                    pause();        }

            }
        }
    }

    private static void viewCart() {
        clearScreen();
        drawCentered("YOUR SHOPPING CART");
        if (cart.isEmpty()) {
            System.out.println("\n    Your cart is empty.");
            pause();
        } else {
            double subtotal = 0;
            for (int i = 0; i < cart.size(); i++) {
                // Format price to 2 decimal places — raw double can print as e.g. $69.990000001
                System.out.printf("    %d. %-30s ($%.2f)%n",
                        (i + 1), cart.get(i).getName(), cart.get(i).getPrice());
                subtotal += cart.get(i).getPrice();
            }
            System.out.printf("%n    Current Total: $%.2f%n", subtotal);
            System.out.print("\n    Enter number to REMOVE item, or press Enter to return: ");
            String choice = sc.nextLine().trim();
            if (!choice.isEmpty()) {
                try { cart.remove(Integer.parseInt(choice) - 1); }
                catch (NumberFormatException e) { /* ignore invalid input */ }
            }
        }
    }

    private static void checkout() {
        if (cart.isEmpty()) {
            System.out.println("    Cart is empty. Nothing to process.");
            pause();
            return;
        }

        // Validate stock for every item BEFORE touching inventory
        // Tally how many of each product are in the cart
        Map<String, Integer> cartCounts = new LinkedHashMap<>();
        for (Product item : cart)
            cartCounts.merge(item.getProductId(), 1, Integer::sum);

        for (Map.Entry<String, Integer> entry : cartCounts.entrySet()) {
            Product p = inventory.get(entry.getKey());
            if (p.getQuantity() < entry.getValue()) {
                System.out.println(RED + "\n    [!] Not enough stock for: " + p.getName()
                        + " (in cart: " + entry.getValue()
                        + ", available: " + p.getQuantity() + ")" + RESET);
                System.out.println("    Please update your cart and try again.");
                pause();
                return;
            }
        }

        // All stock checks passed — process the purchase
        double total = 0;
        for (Product item : cart) {
            total += item.getPrice();
            inventory.get(item.getProductId()).adjustQuantity(-1);
            transactions.add(new Transaction(TransactionType.PURCHASE, item.getProductId(), -1, username));
        }

        cart.clear(); // Empty the cart after successful checkout

        db.saveProducts(inventory);
        db.saveTransactions(transactions);
        System.out.printf(GREEN + "%n    Success! $%.2f charged to %s.%n" + RESET, total, username);
        System.out.println("    Inventory updated and transactions saved.");
        System.out.println("    Thank you for shopping at GAMEHUB!");
        pause();
    }

    // ── ADMIN MENU ────────────────────────────────────────────
    private static void adminMenu() {
        boolean running = true;
        while (running) {
            clearScreen();
            drawCentered("GAMEHUB - ADMIN PANEL");
            System.out.println("\n    " + CYAN + "[1]" + RESET + " Add new product");
            System.out.println("    " + CYAN + "[2]" + RESET + " Remove product");
            System.out.println("    " + CYAN + "[3]" + RESET + " Restock product");
            System.out.println("    " + CYAN + "[4]" + RESET + " Update product price");
            System.out.println("    " + CYAN + "[5]" + RESET + " View all products");
            System.out.println("    " + CYAN + "[6]" + RESET + " View transaction log");
            System.out.println("    " + CYAN + "[7]" + RESET + " Low stock alert");
            System.out.println("    " + CYAN + "[0]" + RESET + " Save & exit");
            System.out.print("\n    Choice: ");
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> adminAddProduct();
                case "2" -> adminRemoveProduct();
                case "3" -> adminRestock();
                case "4" -> adminUpdatePrice();
                case "5" -> adminListProducts();
                case "6" -> adminViewLog();
                case "7" -> adminLowStock();
                case "0" -> {
                    db.saveProducts(inventory);
                    db.saveTransactions(transactions);
                    System.out.println(GREEN + "    Data saved. Goodbye!" + RESET);
                    running = false;
                }
                default -> System.out.println("    Invalid option.");
            }
            if (running) pause();
        }
    }

    private static void adminAddProduct() {
        String id = "";
        while (id.isEmpty()) {
            System.out.print("    Product ID: ");
            id = sc.nextLine().trim();
            if (id.isEmpty()) System.out.println(RED + "    [!] Product ID cannot be empty." + RESET);
        }
        if (inventory.containsKey(id)) {
            System.out.println(RED + "    [!] Product ID already exists." + RESET); return;
        }

        String name = "";
        while (name.isEmpty()) {
            System.out.print("    Product name: ");
            name = sc.nextLine().trim();
            if (name.isEmpty()) System.out.println(RED + "    [!] Product name cannot be empty." + RESET);
        }

        double price = promptDouble("    Price: $");
        int qty      = promptPositiveInt("    Initial quantity: ");
        Product p = new Product(id, name, price, qty);
        inventory.put(id, p);
        transactions.add(new Transaction(TransactionType.ADD, id, qty, username));
        db.saveProducts(inventory);
        db.saveTransactions(transactions);
        System.out.println(GREEN + "    [+] Added: " + p.getDetails() + RESET);
    }

    private static void adminRemoveProduct() {
        System.out.print("    Product ID to remove: ");
        String id = sc.nextLine().trim();
        Product p = inventory.get(id);
        if (p == null) {
            System.out.println(RED + "    [!] Product not found." + RESET); return;
        }
        int removedQty = p.getQuantity();
        inventory.remove(id);
        // Log the actual stock quantity that was removed
        transactions.add(new Transaction(TransactionType.REMOVE, id, -removedQty, username));
        db.saveProducts(inventory);
        db.saveTransactions(transactions);
        System.out.println(GREEN + "    [-] Product " + id + " removed." + RESET);
    }

    private static void adminRestock() {
        System.out.print("    Product ID: ");
        String id = sc.nextLine().trim();
        Product p = inventory.get(id);
        if (p == null) { System.out.println(RED + "    [!] Product not found." + RESET); return; }
        int qty = promptPositiveInt("    Quantity to add: ");
        p.adjustQuantity(qty);
        transactions.add(new Transaction(TransactionType.RESTOCK, id, qty, username));
        db.saveProducts(inventory);
        db.saveTransactions(transactions);
        System.out.println(GREEN + "    [+] Restocked: " + p.getDetails() + RESET);
    }

    private static void adminUpdatePrice() {
        System.out.print("    Product ID: ");
        String id = sc.nextLine().trim();
        Product p = inventory.get(id);
        if (p == null) { System.out.println(RED + "    [!] Product not found." + RESET); return; }
        double price = promptDouble("    New price: $");
        p.updatePrice(price);
        transactions.add(new Transaction(TransactionType.PRICE_UPDATE, id, 0, username));
        db.saveProducts(inventory);
        db.saveTransactions(transactions);
        System.out.println(GREEN + "    [OK] Price updated: " + p.getDetails() + RESET);
    }

    private static void adminListProducts() {
        System.out.println("    +----------------------------------------------------------+");
        System.out.println("    |                 GAMEHUB INVENTORY                        |");
        System.out.println("    +----------------------------------------------------------+");
        for (Product p : inventory.values())
            System.out.println("    " + p.getDetails());
        System.out.println("    +----------------------------------------------------------+");
        System.out.println("    Total products: " + inventory.size());
    }

    private static void adminViewLog() {
        System.out.println("    +----------------------------------------------------------+");
        System.out.println("    |                 TRANSACTION LOG                          |");
        System.out.println("    +----------------------------------------------------------+");
        if (transactions.isEmpty()) {
            System.out.println("    No transactions recorded yet.");
        } else {
            for (Transaction t : transactions)
                System.out.println("    " + t);
        }
        System.out.println("    +----------------------------------------------------------+");
        System.out.println("    Total transactions: " + transactions.size());
    }

    private static void adminLowStock() {
        int threshold;
        while (true) {
            threshold = promptInt("    Low stock threshold: ");
            if (threshold >= 0) break;
            System.out.println(RED + "    [!] Threshold must be 0 or greater." + RESET);
        }
        System.out.println("    [!] Products at or below " + threshold + " units:");
        boolean found = false;
        for (Product p : inventory.values()) {
            if (p.getQuantity() <= threshold) {
                System.out.println("    " + p.getDetails());
                found = true;
            }
        }
        if (!found) System.out.println("    All products sufficiently stocked.");
    }

    // ── Helpers ───────────────────────────────────────────────
    private static double promptDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Double.parseDouble(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("    [!] Enter a valid number."); }
        }
    }

    private static int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("    [!] Enter a whole number."); }
        }
    }

    // Rejects 0 and negative values — used for quantities that must be positive
    private static int promptPositiveInt(String prompt) {
        while (true) {
            int val = promptInt(prompt);
            if (val > 0) return val;
            System.out.println("    [!] Value must be greater than 0.");
        }
    }

    private static void drawCentered(String text) {
        int width   = 70;
        int padding = Math.max(0, (width - text.length()) / 2);
        for (int i = 0; i < padding; i++) System.out.print(" ");
        System.out.println(GOLD + text + RESET);
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void pause() {
        System.out.print("\n    Press Enter to continue...");
        sc.nextLine();
    }
}