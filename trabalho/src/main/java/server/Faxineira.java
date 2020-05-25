package server;

import business.data.customer.CustomerDAO;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class Faxineira {
    private CustomerDAO customerDAO;
    private Duration tmax;
    private Date nextClean;
    private Calendar calendar;

    public Faxineira(CustomerDAO customerDAO, Duration tmax) {
        this.customerDAO = customerDAO;
        this.tmax = tmax;
        this.nextClean = Date.from(Instant.MIN);
        this.calendar = Calendar.getInstance();
    }

    public boolean limpar(Duration tmax) {
        if(this.calendar.getTime().before(nextClean)) {
            return false;
        }
        // TODO: limpar a base de dados
        return true;
    }
}
