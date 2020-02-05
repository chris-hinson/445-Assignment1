//Christopher Hinson
//SimBank class for cs0445 Assignment1

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

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
    //max customers per line
    int max_per_line;

    //ArrayList of SUCCESSFULLY served customers
    private ArrayList<Customer> succ_serve = new ArrayList<Customer>();
    //ArrayList of NOT SERVED customers
    private ArrayList<Customer> not_succ_serve = new ArrayList<Customer>();

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

        max_per_line = maxq/numOfQs;
    }

    public void runSimulation()
    {
    //////////////////////////// BANK SETUP /////////////////////////
        //arraylist of queues representing each line
        ArrayList<Queue> lines = new ArrayList<Queue>();
        //fill arraylist with linklists to represent each queue
        for (int i = 0; i <numOfQs;i++)
        {
            lines.add(new LinkedList<Customer>());
        }

        //make an Array of Tellers
        Teller[] tellers = new Teller[lines.size()];
        //fill teller array with tellers
        for (int i = 0; i< tellers.length; i++)
        {
            tellers[i] = new Teller(i);
        }

        //current time is 0, as we havent started yet
        double curr_time = 0.0;
        //minutes bank is open
        double min_open = hrs*60;
        //close time
        double close_time = curr_time + min_open;
        //arrival rate / min
        double arr_rate_min = arr_rate/60;
        //transaction rate/ min
        double trans_rate = 1/t_min;
        //people in bank
        int people_in_bank = 0;



    ///////////////////////////// END BANK SETUP ///////////////////////
    //////////////////////////// MAIN LOOP /////////////////////////////

        //customer ID. Gets incremented every time we make a customer
        int custID = 0;

        //Future Event List
        PriorityQueue<SimEvent> FEL= new PriorityQueue<SimEvent>();

        //generates our first arrival event and adds it to FEL
        double next_arr_min = R.exponential(arr_rate_min);
        ArrivalEvent next_arrival = new ArrivalEvent(next_arr_min);
        FEL.offer(next_arrival);

        //while we have future events to be processed
        while (FEL.size() > 0)
        {
            // Get the next event from the FEL
            SimEvent curr = FEL.poll();
            curr_time = curr.get_e_time(); // Move clock up to time of next
            // event.  The idea is that the clock moves in chunks
            // of time -- up to the time when the next event will
            // occur. The time in between can be skipped because
            // nothing is overtly occurring / changing during this
            // time.
            //System.out.printf("\tEvent at %6.2f ", curr.get_e_time());

            ///////////////////////////////////////////ARRIVAL EVENT/////////////////////////////////////
            // Check to see the type of event using instanceof operator
            if (curr instanceof ArrivalEvent)
            {
                //increment customer ID
                custID++;
                //add one to people in bank
                people_in_bank++;
                //output arrivalevent
                //System.out.print(" is ArrivalEvent");

                //make the customer object for this arrival event
                Customer curr_cust = new Customer(custID,next_arr_min);

                //calculate the service interation time
                //this can be generated before actual interaction happens e.g., when a customer is put in a line
                double serve_time = R.exponential(trans_rate);
                //System.out.printf(": Service time: %6.2f\n", serve_time);
                //give customer object its service time
                curr_cust.setServiceT(serve_time);

                //only worry about tellers and or queues if under max people allowed in bank
                if (people_in_bank+1 <=maxq) {
                    //////////////////TELLER STUFF //////////////////////////

                    boolean teller_found = false;

                    //look for free teller
                    for (int i = 0; i < tellers.length; i++) {
                        //if teller is free, add customer to them
                        if (!tellers[i].isBusy()) {
                            //add customer to teller
                            tellers[i].addCust(curr_cust);
                            //add teller to custpmer
                            curr_cust.setTeller(i);

                            //set service start time to now
                            curr_cust.setStartT(curr_time);

                            //calculate the finish time of the customer
                            double finish_time = curr_time + serve_time;
                            //give customer their completion time
                            curr_cust.setEndT(finish_time);
                            //make a completion loc event at finish time with correct teller
                            CompletionLocEvent next_complete = new CompletionLocEvent(finish_time, i);
                            //System.out.printf("\t\tAdding CompletionEvent for time: %6.2f \n", finish_time);
                            FEL.offer(next_complete);  // Add CompletionEvent to PQ

                            //add customer to completed arraylist
                            succ_serve.add(curr_cust);

                            //set sentinel value for finding queue to true
                            teller_found = true;
                            //break if sucessfully interaction
                            break;
                        }
                    }

                    ////////////////////////// END TELLER STUFF ////////////////////////////////////

                    //we only do queue stuff if no teller has been found
                    if (teller_found == false)
                    {
                        //////////////////////////// QUEUE STUFF ///////////////////////////////////////
                        //gets index of current shortest line
                        //if there are multiple shortest, chooses the rightmost
                        Queue curr_shortest_line = lines.get(0);
                        int index_of_shortest_line = 0;
                        //look for shortest line if no f
                        for (int i = 0; i < lines.size(); i++) {
                            if (lines.get(i).size() < curr_shortest_line.size()) {
                                curr_shortest_line = lines.get(i);
                                index_of_shortest_line = i;
                            }
                        }

                        //add the customer to the shortest
                        lines.get(index_of_shortest_line).offer(curr_cust);
                        curr_cust.setQueue(index_of_shortest_line);

                        ////////////////////////// END QUEUE STUFF ////////////////////////////////////////
                    }
                }
                else
                //add customer to failed arraylist
                {
                    not_succ_serve.add(curr_cust);
                    people_in_bank--;
                }


                //////////////////////// GENERATE NEXT ARRIVAL /////////////////////////////////////////
                //next arrival time
                next_arr_min = curr_time + R.exponential(arr_rate_min);

                //only add an arrival event if the generated arrival time is before closing
                if (next_arr_min <= close_time)
                {
                    next_arrival = new ArrivalEvent(next_arr_min);
                    //System.out.printf("\t\tAdding next ArrivalEvent for time: %6.2f \n", next_arr_min);
                    FEL.offer(next_arrival);
                }
            }
            else
            {
                //System.out.println(" is a CompletionEvent");

                people_in_bank--;

                //free up teller
                int freed_teller = ((CompletionLocEvent)curr).getLoc();
                tellers[freed_teller].removeCust();

                //check if theres another customer in this queue
                if (lines.get(freed_teller).size()>0)
                {
                    //TODO this seems fishy
                    Customer curr_cust = (Customer) lines.get(freed_teller).poll();
                    //set start service time to now
                    curr_cust.setStartT(curr_time);

                    //add customer to teller
                    tellers[freed_teller].addCust(curr_cust);
                    //add teller to custpmer
                    curr_cust.setTeller(freed_teller);

                    //calculate the finish time of the customer
                    double finish_time = curr_time + curr_cust.getServiceT();
                    //give customer their completion time
                    curr_cust.setEndT(finish_time);
                    //make a completion loc event at finish time with correct teller
                    CompletionLocEvent next_complete = new CompletionLocEvent(finish_time, freed_teller);
                    //System.out.printf("\t\tAdding CompletionEvent for time: %6.2f \n", finish_time);
                    FEL.offer(next_complete);  // Add CompletionEvent to PQ
                    //add customer to completed arraylist
                    succ_serve.add(curr_cust);
                }

            }
        }

    }

    public void showResults()
    {
        /////////////////////Successful customers////////////////////////////////
        System.out.println("Customer  Arrival    Service  Queue  Teller  Time Serv  Time Cust  Time Serv  Time Spent ");
        System.out.println("   Id      Time       Time     Loc    Loc     Begins      Waits      Ends       in Sys   ");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (int i = 0;i<succ_serve.size();i++)
        {
            Customer curr_cust = succ_serve.get(i);
            System.out.println(curr_cust.getId() + "    " + curr_cust.getArrivalT() + " " + curr_cust.getServiceT() + "    " +
                    curr_cust.getQueue() + "    " + curr_cust.getTeller() + "   " + curr_cust.getStartT() + "   " +
                    curr_cust.getWaitT() + "    " + curr_cust.getEndT() + "    " +curr_cust.getInSystem());
        }


        //////////////////// Non-successful/////////////////////////////

        System.out.println("Customers Who did not stay");
        System.out.println("\n\n");
        System.out.println("Customer  Arrival    Service");
        System.out.println("   Id      Time       Time  ");
        System.out.println("--------  -------    -------");

        for (int i = 0; i<not_succ_serve.size();i++)
        {
            Customer curr_cust = not_succ_serve.get(i);
            System.out.println(curr_cust.getId() + "    " + curr_cust.getArrivalT() + "    " + curr_cust.getServiceT());
        }
    }

}
