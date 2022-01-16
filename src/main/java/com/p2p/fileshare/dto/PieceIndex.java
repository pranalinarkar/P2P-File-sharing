package com.p2p.fileshare.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class PieceIndex implements Delayed
{
    private final int index;
    private final LocalDateTime insertTime;

    public PieceIndex(int index)
    {
        this.index = index;
        this.insertTime = LocalDateTime.now().plusSeconds(30);
    }

    public int getIndex()
    {
        return index;
    }

    @Override
    public long getDelay(TimeUnit unit)
    {
        LocalDateTime now = LocalDateTime.now();
        long diff = now.until(insertTime, ChronoUnit.MILLIS);
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o)
    {
        long result = this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);

        if (result < 0)
        {
            return -1;
        }
        else if (result > 0)
        {
            return 1;
        }

        return 0;
    }
}
