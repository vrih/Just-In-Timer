package com.danbowtell.justintimer;

public class FormattedNumber
{
    private int iHours;
    private int iMinutes;
    private int iSeconds;
    private static StringBuilder output = new StringBuilder();

    public FormattedNumber(){}

    public void calculateTime(Long millis)
    {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = (seconds % 60);
        minutes = minutes % 60;
        hours = hours % 60;

        this.iHours = (int) hours;
        this.iMinutes = (int) minutes;
        this.iSeconds = (int) seconds;
    }

    public int getHours()
    {
        return this.iHours;
    }

    public int getMinutes()
    {
        return this.iMinutes;
    }

    public int getSeconds()
    {
        return this.iSeconds;
    }

    public String getDisplay(long millis)
    {
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        seconds = (seconds % 60);
        minutes = minutes % 60;
        hours = hours % 60;

        output.setLength(0);

        if(hours > 0)
        {
               output.append(hours).append(":");
        }

        if(minutes < 10)
        {
            output.append(0).append(minutes);
        }
        else
        {
            output.append(minutes);
        }

        output.append(":");

        if(seconds < 10)
        {
            output.append(0).append(seconds);
        }
        else
        {
            output.append(seconds);
        }

        return output.toString();
    }
}
