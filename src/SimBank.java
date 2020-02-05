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

    //arraylist of queues representing each line
    ArrayList<Queue> lines = new ArrayList<Queue>();
    //make an Array of Tellers
    Teller[] tellers;

    //this is a variable for counting how many people must wait in a queue(dont go straight to a teller)
    private int waiters = 0;

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


        //fill arraylist with linklists to represent each queue
        for (int i = 0; i <numOfQs;i++)
        {
            lines.add(new LinkedList<Customer>());
        }

        //make teller at the right size
         tellers = new Teller[ntell];

        //fill teller array with tellers
        for (int i = 0; i< tellers.length; i++)
        {
            tellers[i] = new Teller(i);
        }
    }

    public void runSimulation()
    {
    //////////////////////////// BANK SETUP /////////////////////////
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
            ///////////////////////////////////////////ARRIVAL EVENT/////////////////////////////////////
            // Check to see the type of event using instanceof operator
            if (curr instanceof ArrivalEvent)
            {
                //increment customer ID
                custID++;
                //output arrivalevent
                //System.out.print(" is ArrivalEvent");

                //make the customer object for this arrival event
                Customer curr_cust = new Customer(custID,curr_time);

                //calculate the service interation time
                //this can be generated before actual interaction happens e.g., when a customer is put in a line
                double serve_time = R.exponential(trans_rate);
                //give customer object its service time
                curr_cust.setServiceT(serve_time);

                //find people in bank M
                people_in_bank = 0;
                for (int i = 0; i<lines.size();i++)
                {
                    people_in_bank +=lines.get(i).size();
                }

                //only worry about tellers and or queues if under max people allowed in bank
                if (people_in_bank <maxq) {
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
                    if (!teller_found)
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
                        waiters++;

                        ////////////////////////// END QUEUE STUFF ////////////////////////////////////////
                    }
                }
                else
                //add customer to failed arraylist
                {
                    not_succ_serve.add(curr_cust);
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
                //free up teller
                int freed_teller = ((CompletionLocEvent)curr).getLoc();
                tellers[freed_teller].removeCust();

                //find correct bank scenario
                if (ntell == lines.size()) {
                    if (lines.get(freed_teller).size() > 0) {
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
                else
                {
                    if(lines.get(0).size()>0)
                    {
                        Customer curr_cust = (Customer) lines.get(0).poll();
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

    }

    public void showResults()
    {
        /////////////////////Successful customers////////////////////////////////

        System.out.println("Individual customer service information: \n");

        System.out.println("Customer  Arrival    Service  Queue  Teller  Time Serv  Time Cust  Time Serv  Time Spent ");
        System.out.println("   Id      Time       Time     Loc    Loc     Begins      Waits      Ends       in Sys   ");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (int i = 0;i<succ_serve.size();i++)
        {
            Customer curr_cust = succ_serve.get(i);
            System.out.print("  ");
            System.out.print(curr_cust.getId() + "    ");
            System.out.printf("%.2f",curr_cust.getArrivalT());
            System.out.print("      ");
            System.out.printf("%.2f",curr_cust.getServiceT());
            System.out.print("      ");
            System.out.print(curr_cust.getQueue() + "      ");
            System.out.print(curr_cust.getTeller()+ "     ");
            System.out.printf("%.2f",curr_cust.getStartT() );
            System.out.print("      ");
            System.out.printf("%.2f",curr_cust.getWaitT());
            System.out.print("      ");
            System.out.printf("%.2f",curr_cust.getEndT() );
            System.out.print("      ");
            System.out.printf("%.2f",curr_cust.getInSystem());
            System.out.print("\n");
        }

        System.out.print("\n\n\n");

        //////////////////// Non-successful/////////////////////////////

        System.out.println("Customers Who did not stay");
        System.out.println("\n\n");
        System.out.println("Customer  Arrival    Service");
        System.out.println("   Id      Time       Time  ");
        System.out.println("--------  -------    -------");

        for (int i = 0; i<not_succ_serve.size();i++)
        {
            Customer curr_cust = not_succ_serve.get(i);
            System.out.print(curr_cust.getId() + "    ");
            System.out.printf("%.2f",curr_cust.getArrivalT());
            System.out.print("      ");
            System.out.printf("%.2f",curr_cust.getServiceT());
            System.out.print("\n");
        }

        System.out.print("\n\n\n");

        //////////////////////////////STATS ////////////////////////////////////



        //Number of tellers
        System.out.println("Number of Tellers: " + tellers.length);
        //Number of Qs
        System.out.println("Number of Queues: " + numOfQs);
        //max number allowed to wait
        System.out.println("Max number allowed to wait: " + maxq);
        //Customer arrival rate (per hr):
        System.out.println("Customer arrival rate (per hr): " + arr_rate);
        //Customer service time (ave min)
        System.out.println("Customer service time (ave min): " + t_min);
        //num of customers who arrives
        System.out.println("Number of customers arrived: " + (succ_serve.size()+not_succ_serve.size()));
        //number of customers served
        System.out.println("Number of customers served: " + succ_serve.size());
        //Num. Turned Away
        System.out.println("Num. Turned Away: " + not_succ_serve.size());
        //num who waited
        System.out.println("Num. who waited: " + waiters);
        //average wait
        double tot_waiting_time = 0;
        for (int i=0;i<succ_serve.size();i++)
        {
            Customer curr_cust = succ_serve.get(i);
            tot_waiting_time += curr_cust.getWaitT();
        }
        int total_people = succ_serve.size();
        double ave_wait_time = tot_waiting_time/total_people;
        System.out.println("Average Wait: " + ave_wait_time);
        //Max wait
        double highest_wait = 0.0;
        for (int i=0;i<succ_serve.size();i++)
        {
            if (succ_serve.get(i).getWaitT() > highest_wait)
            {
                highest_wait = succ_serve.get(i).getWaitT();
            }
        }
        System.out.println("Max Wait: " + highest_wait);

        //Standard Deviation
        ArrayList<Double> blech = new ArrayList<Double>();
        for (int i=0;i<succ_serve.size();i++)
        {
            //make an array of the wait times-mean^2
            blech.add((succ_serve.get(i).getWaitT()-ave_wait_time)*(succ_serve.get(i).getWaitT()-ave_wait_time));
        }
            //sum of squared differences
        double array_total = 0.0;
        for (int i=0;i<blech.size();i++)
        {
            array_total += blech.get(i);
        }
        double deviance = array_total/succ_serve.size();
        deviance = Math.sqrt(deviance);
        System.out.println("Std. Dev. Wait: " + deviance);

        //Average Service
        double total_service_time = 0;
        for (int i=0;i<succ_serve.size();i++)
        {
            total_service_time += succ_serve.get(i).getServiceT();
        }
        double average_serv_time = total_service_time/succ_serve.size();
        System.out.println("Ave. Service: " + average_serv_time);
        //Average Waiter Wait
        double waiter_wait_time = 0;
        for (int i=0;i<succ_serve.size();i++)
        {
            Customer curr_cust = succ_serve.get(i);
            if (curr_cust.getWaitT() != 0)
            {
                waiter_wait_time += curr_cust.getWaitT();
            }
        }
        double ave_waiter_wait_time = waiter_wait_time/waiters;
        System.out.println("Average Waiter Wait: " + ave_waiter_wait_time);
        //average in system
        double tot_in_system = 0;
        for (int i=0;i<succ_serve.size();i++)
        {
            Customer curr_cust = succ_serve.get(i);
            tot_in_system += curr_cust.getInSystem();
        }
        double ave_wait_in_system = tot_in_system/succ_serve.size();
        System.out.println("Ave. in System: " + ave_wait_in_system);




    }

}
