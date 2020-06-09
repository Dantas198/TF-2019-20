package server;

import business.customer.Customer;
import business.data.customer.CustomerDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderDAO;
import business.order.Order;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class CurrentOrderCleaner {
    private CustomerSQLDAO customerSQLDAO;
    private Duration tmax;
    private Date nextClean;
    private Calendar calendar;

    public CurrentOrderCleaner(CustomerSQLDAO customerDAO, Duration tmax) {
        this(customerDAO, tmax, Calendar.getInstance());
    }

    public CurrentOrderCleaner(CustomerSQLDAO customerDAO, Duration tmax, Calendar calendar) {
        this.customerSQLDAO = customerDAO;
        this.tmax = tmax;
        this.nextClean = null;
        this.calendar = calendar;
    }

    public boolean clean() throws SQLException {
        if(nextClean != null && this.calendar.getTime().before(nextClean)) {
            return false;
        }
        nextClean = null;
        // TODO: limpar a base de dados
        Date currentTime = this.calendar.getTime();
        for(Customer customer : customerSQLDAO.getAll().values()) {
            Order currentOrder = customer.getCurrentOrder();
            if(currentOrder == null) continue;
            Date orderTimestamp = currentOrder.getTimestamp();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(orderTimestamp);
            // TODO: Potencial erro ao dar cast para int
            calendar.add(Calendar.SECOND, (int) tmax.toSeconds());
            System.out.println("Expiration: " + calendar.getTime());
            System.out.println(currentTime + " " + calendar.getTime());
            if(currentTime.after(calendar.getTime())) {
                System.out.println("APAGAR " + customer.getId());
                customer.deleteCurrentOrder();
                System.out.println(customer);
                System.out.println(customerSQLDAO.get("Customer 1").getCurrentOrder());
                System.out.println("order??" + customer.getCurrentOrder());
            } else {
                if(nextClean == null || orderTimestamp.before(nextClean)) {
                    nextClean = orderTimestamp;
                }
            }
        }
        return true;
    }

    public Date getNextClean() {
        return nextClean;
    }
}
