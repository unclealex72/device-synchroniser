package uk.co.unclealex.sync.devicesynchroniser.dates;

import java.util.Date;

/**
 * Created by alex on 26/12/14.
 */
public interface Iso8601 {

    public String format(Date date);

    public Date parse(String date);
}
