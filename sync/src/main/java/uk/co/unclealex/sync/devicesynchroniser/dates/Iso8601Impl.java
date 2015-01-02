package uk.co.unclealex.sync.devicesynchroniser.dates;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alex on 26/12/14.
 */
public class Iso8601Impl implements Iso8601 {

    public DateFormat df() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    @Override
    public String format(Date date) {
        return df().format(date);
    }

    @Override
    public Date parse(String date) {
        try {
            return df().parse(date.replace("Z", "+0000"));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse date " + date, e);
        }
    }
}
