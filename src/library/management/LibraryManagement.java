package library.management;

import config.config;
import java.util.*;

public class LibraryManagement {

    public static void viewbook(config db) {
        String query = "SELECT * FROM tbl_Librarian";
        String[] headers = {"bid", "title", "names", "date", "genre"};
        String[] columns = {"bid", "title", "names", "date", "genre"};
        db.viewRecords(query, headers, columns);
    }

    public static void viewUsers(config db) {
        String query = "SELECT * FROM tbl_log WHERE u_status != 'Approved'";
        String[] headers = {"u_id", "u_email", "u_type", "u_status"};
        String[] columns = {"u_id", "u_email", "u_type", "u_status"};
        db.viewRecords(query, headers, columns);
    }

    public static void viewPenalties(config db) {
        String query = "SELECT * FROM tbl_penalty";
        String[] headers = {"penalty_id", "b_name", "b_email", "b_id", "b_days", "b_penal", "b_dpenal"};
        String[] columns = {"penalty_id", "b_name", "b_email", "b_id", "b_days", "b_penal", "b_dpenal"};
        db.viewRecords(query, headers, columns);
    }

    // ✅ FIXED: SHOWS BORROWER NAME + BOOK DETAILS from tbl_borrowed
    public static void viewAllBorrowedBooksWithDetails(config db) {
        String query = "SELECT DISTINCT tl.b_name, tb.borrower_email, tb.book_id, tl2.title, tb.borrow_date, tb.borrow_duration " +
                      "FROM tbl_borrowed tb " +
                      "JOIN tbl_log tl ON tb.borrower_email = tl.u_email " +
                      "JOIN tbl_Librarian tl2 ON tb.book_id = tl2.bid " +
                      "WHERE tb.status = 'Borrowed'";
        String[] headers = {"borrower_name", "email", "book_id", "book_title", "borrow_date", "days_agreed"};
        String[] columns = {"b_name", "borrower_email", "book_id", "title", "borrow_date", "borrow_duration"};
        db.viewRecords(query, headers, columns);
    }

    // ✅ FIXED: SHOWS BORROWER'S OWN BOOKS with titles
    public static void viewBorrowedBooks(config db, String email) {
        String query = "SELECT tb.book_id, tl.title, tb.borrow_date, tb.borrow_duration " +
                      "FROM tbl_borrowed tb " +
                      "JOIN tbl_Librarian tl ON tb.book_id = tl.bid " +
                      "WHERE tb.borrower_email = '" + email + "' AND tb.status = 'Borrowed'";
        String[] headers = {"book_id", "book_title", "borrow_date", "days_agreed"};
        String[] columns = {"book_id", "title", "borrow_date", "borrow_duration"};
        db.viewRecords(query, headers, columns);
    }

    public static void addBook(config db, String title, String author, String date, String genre) {
        String sql = "INSERT INTO tbl_Librarian (title, names, date, genre) VALUES (?, ?, ?, ?)";
        db.addRecord(sql, title, author, date, genre);
    }

    public static void editBook(config db, int id, String title, String author, String date, String genre) {
        String sql = "UPDATE tbl_Librarian SET title = ?, names = ?, date = ?, genre = ? WHERE bid = ?";
        db.updateRecord(sql, title, author, date, genre, id);
    }

    public static void deleteBook(config db, int id) {
        String sql = "DELETE FROM tbl_Librarian WHERE bid = ?";
        db.deleteRecord(sql, id);
    }

    public static void approveUser(config db, String email) {
        String sql = "UPDATE tbl_log SET u_status = 'Approved' WHERE u_email = ?";
        db.updateRecord(sql, email);
    }

    public static void processReturn(config db, int bookId, int actualDays, String returnDate) {
        String findSql = "SELECT tb.borrower_email, tb.borrow_duration FROM tbl_borrowed tb WHERE tb.book_id = ? AND tb.status = 'Borrowed'";
        List<Map<String, Object>> info = db.fetchRecords(findSql, bookId);

        if (!info.isEmpty()) {
            String email = info.get(0).get("borrower_email").toString();
            int agreedDays = Integer.parseInt(info.get(0).get("borrow_duration").toString());
            int lateDays = actualDays - agreedDays;

            // Get borrower name for penalty
            String nameSql = "SELECT b_name FROM tbl_log WHERE u_email = ?";
            List<Map<String, Object>> nameInfo = db.fetchRecords(nameSql, email);
            String borrowerName = nameInfo.isEmpty() ? "Unknown" : nameInfo.get(0).get("b_name").toString();

            // Update borrowed status
            String updateSql = "UPDATE tbl_borrowed SET status = 'Returned', return_date = ?, actual_borrow_days = ? WHERE borrower_email = ? AND book_id = ?";
            db.updateRecord(updateSql, returnDate, actualDays, email, bookId);

            // Add penalty if late
            if (lateDays > 0) {
                double penalty = lateDays * 10;
                String penaltySql = "INSERT INTO tbl_penalty (b_name, b_email, b_id, b_days, b_penal, b_dpenal) VALUES (?, ?, ?, ?, ?, NOW())";
                db.addRecord(penaltySql, borrowerName, email, bookId, lateDays, penalty);
                System.out.println("Penalty added: P" + penalty + " for " + lateDays + " late days");
            } else {
                System.out.println("No penalty - returned on time!");
            }
        } else {
            System.out.println("Book not currently borrowed!");
        }
    }

    public static void borrowBook(config db, String email, int bookId, String borrowDate, int days) {
        String checkSql = "SELECT * FROM tbl_borrowed WHERE borrower_email = ? AND book_id = ? AND status = 'Borrowed'";
        List<Map<String, Object>> already = db.fetchRecords(checkSql, email, bookId);
        
        if (already.isEmpty()) {
            String sql = "INSERT INTO tbl_borrowed (borrower_email, book_id, status, borrow_date, borrow_duration) VALUES (?, ?, ?, ?, ?)";
            db.addRecord(sql, email, bookId, "Borrowed", borrowDate, days);
            System.out.println("Book borrowed for " + days + " days!");
        } else {
            System.out.println("You already have this book!");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        config db = new config();
        
        while (true) {
            System.out.println("\n=== Library System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            int ch = sc.nextInt();
            sc.nextLine();

            if (ch == 3) {
                System.out.println("Goodbye!");
                break;
            }

            if (ch == 1) {
                System.out.print("Email: ");
                String email = sc.nextLine();
                System.out.print("Password: ");
                String password = sc.nextLine();

                // ✅ HASHED LOGIN
                String hashedPass = config.hashPassword(password);
                if (hashedPass == null) {
                    System.out.println("Hashing error!");
                    continue;
                }

                String loginSql = "SELECT * FROM tbl_log WHERE u_email = ? AND u_pass = ? AND u_status = 'Approved'";
                List<Map<String, Object>> result = db.fetchRecords(loginSql, email, hashedPass);

                if (result.isEmpty()) {
                    System.out.println("Login failed!");
                    continue;
                }

                String userType = result.get(0).get("u_type").toString();
                System.out.println("Welcome " + userType + "!");

                if (userType.equalsIgnoreCase("Admin")) {
                    System.out.println("1. Approve User");
                    System.out.println("2. View Penalties");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        viewUsers(db);
                        System.out.print("Email to approve: ");
                        String userEmail = sc.nextLine();
                        approveUser(db, userEmail);
                        System.out.println("Approved!");
                    } 
                    else if (choice == 2) {
                        viewPenalties(db);
                    }
                } 
                else if (userType.equalsIgnoreCase("Librarian")) {
                    System.out.println("1. Add Book");
                    System.out.println("2. Edit Book");
                    System.out.println("3. View Books");
                    System.out.println("4. Delete Book");
                    System.out.println("5. Process Return");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        System.out.print("Title: ");
                        String title = sc.nextLine();
                        System.out.print("Author: ");
                        String author = sc.nextLine();
                        System.out.print("Date: ");
                        String date = sc.nextLine();
                        System.out.print("Genre: ");
                        String genre = sc.nextLine();
                        addBook(db, title, author, date, genre);
                    }
                    else if (choice == 2) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        System.out.print("New Title: ");
                        String title = sc.nextLine();
                        System.out.print("New Author: ");
                        String author = sc.nextLine();
                        System.out.print("New Date: ");
                        String date = sc.nextLine();
                        System.out.print("New Genre: ");
                        String genre = sc.nextLine();
                        editBook(db, id, title, author, date, genre);
                    }
                    else if (choice == 3) {
                        viewbook(db);
                    }
                    else if (choice == 4) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        deleteBook(db, id);
                    }
                    else if (choice == 5) {
                        System.out.println("\n--- ALL BORROWED BOOKS (WHO BORROWED WHAT) ---");
                        viewAllBorrowedBooksWithDetails(db);
                        System.out.print("Book ID to return: ");
                        int bookId = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Actual Days borrowed: ");
                        int actualDays = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Return Date (YYYY-MM-DD): ");
                        String returnDate = sc.nextLine();
                        processReturn(db, bookId, actualDays, returnDate);
                    }
                } 
                else {
                    // Borrower
                    System.out.println("1. View Books");
                    System.out.println("2. My Books");
                    System.out.println("3. Borrow Book");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        viewbook(db);
                    }
                    else if (choice == 2) {
                        System.out.println("\n--- YOUR BORROWED BOOKS ---");
                        viewBorrowedBooks(db, email);
                    }
                    else if (choice == 3) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int bookId = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Borrow Date (YYYY-MM-DD): ");
                        String borrowDate = sc.nextLine();
                        System.out.print("Days: ");
                        int days = sc.nextInt();
                        sc.nextLine();
                        borrowBook(db, email, bookId, borrowDate, days);
                    }
                }
            } 
            else if (ch == 2) {
                System.out.print("Name: ");
                String name = sc.nextLine();
                System.out.print("Email: ");
                String email = sc.nextLine();
                System.out.print("Password: ");
                String pass = sc.nextLine();

                String checkSql = "SELECT * FROM tbl_log WHERE u_email = ?";
                List<Map<String, Object>> exists = db.fetchRecords(checkSql, email);
                
                if (!exists.isEmpty()) {
                    System.out.println("Email exists!");
                    continue;
                }

                // ✅ HASHED REGISTER
                String hashedPass = config.hashPassword(pass);
                if (hashedPass == null) {
                    System.out.println("Hashing error!");
                    continue;
                }

                String insertSql = "INSERT INTO tbl_log (b_name, u_email, u_pass, u_type, u_status) VALUES (?, ?, ?, ?, ?)";
                db.addRecord(insertSql, name, email, hashedPass, "Borrower", "Pending");
                System.out.println("Registered! Wait for approval.");
            }
        }

        sc.close();
    }
}