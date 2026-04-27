# Inventory Management System - GameHub

**OOP Final Project | Spring 2026**  
**Team:** Ishaan Thapa & Mahmoud Mohamed

## Description
A Java-based inventory management system for a gaming store called GameHub.
Tracks products, records purchases, and timestamps all user activity automatically.

## Features
- Add, remove, restock, and update product prices
- Auto-timestamped transaction log on every action
- Customer shopping with cart and checkout
- Admin panel for full inventory control
- Low stock alerts
- Data saved to CSV files between sessions

## How to Run
javac shoppingapp.java
java shoppingapp

## Modes
- Customer mode — browse products, add to cart, checkout
- Admin mode — manage inventory, view transaction log, low stock alert

## Files
- shoppingapp.java — main application
- data/products.csv — saved product inventory
- data/transactions.csv — saved transaction history
- Analysis_Document.docx — full system analysis document

## Technologies
- Java 8+
- CSV file storage for data persistence
- LocalDateTime for automatic timestamping
