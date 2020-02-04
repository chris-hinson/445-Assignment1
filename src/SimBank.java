//Christopher Hinson
//SimBank class for cs0445 Assignment1

import java.util.PriorityQueue;

public class SimBank
{
    //number of tellers
    private int ntell;
    //number of hours to run simulation (double)
    private double hrs;
    //arrival rate of customers (double, in arrivals per hour)
    private double arr_rate;
    //ave transaction length (double, in minutes)
    private double t_min;
    //max customers allowed to wait (int, not counting those being served)
    private int maxq;
    //number of Qs
    private int numOfQs;
    //RandDist for real-life random Number Gen
    RandDist R;

    public SimBank(int ntell, boolean qtype, double hrs, double arr_rate, double t_min, int maxq, long seed)
    {
        this.ntell = ntell;
        this.hrs = hrs;
        this.arr_rate = arr_rate;
        this.t_min = t_min;
        this.maxq = maxq;

        if (qtype)
        {
            numOfQs = 1;
        }
        else
        {
            numOfQs = ntell;
        }

        R = new RandDist(seed);
    }

    public void runSimulation()
    {

        PriorityQueue<Customer> PQ= new PriorityQueue<Customer>();

    }

    public void showResults()
    {

    }

}
