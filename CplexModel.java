
//import Data.InputData;
//import java.util.Arrays;
//
//import ilog.concert.IloException;
//import ilog.concert.IloIntVar;
//import ilog.concert.IloLinearIntExpr;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.cplex.IloCplex;
//
///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
///**
// *
// * @author Othmane
// */
//public class CplexModel {
//    public IloIntVar[][] U;
//    public IloNumVar[] V;
//    public IloCplex cplex;
//    
//    public CplexModel(InputData data) throws IloException{
//        this.cplex = new IloCplex();
//        this.U = new IloIntVar[data.StopsCount][];
//        this.V = new IloIntVar[data.StopsCount];
//        for(int k = 0; k < this.U.length; k++)
//            this.U[k] = this.cplex.boolVarArray(data.StopsCount);
//        this.V = this.cplex.numVarArray(data.StopsCount, 0, data.StopsCount - 1);
//        this.modelDefinition(data);
//    }
//    
//    public void modelDefinition(InputData data) throws IloException{
//        this.objective(data);
//        this.contrainte1();
//        this.contrainte2(data);
//    }
//    
//    public void objective(InputData data) throws IloException{
//        IloNumExpr exp=this.cplex.numExpr();
//        for(int k = 0; k < this.U.length; k++)
//            for(int l = 0; l < this.U.length; l++)
//                exp=this.cplex.sum(this.cplex.prod(data.getCost(k, l), this.U[k][l]), exp);
//        this.cplex.addMinimize(exp);
//    }
//    
//    public void contrainte1() throws IloException {
//        for (int i = 0; i < this.U.length; i++) {
//            IloLinearIntExpr exp = this.cplex.linearIntExpr();
//            for(int k = 0; k < this.U.length; k++){
//                if(k == i)
//                    continue;
//                exp.addTerm(this.U[i][k], 1);
//            }
//            this.cplex.addEq(exp,1);
//        }
//        for (int i = 0; i < this.U.length; i++) {
//            IloLinearIntExpr exp = this.cplex.linearIntExpr();
//            for(int k = 0; k < this.U.length; k++){
//                if(k == i)
//                    continue;
//                exp.addTerm(this.U[k][i], 1);
//            }
//            this.cplex.addEq(exp,1);
//        }
//    }
//    
//    public void contrainte2(InputData data) throws IloException{
//        for (int k = 1; k < this.U.length; k++)
//            for (int l = 1; l < this.U.length; l++) {
//                if (k != l) {
//                    IloLinearNumExpr exp=this.cplex.linearNumExpr();
//                    exp.addTerm(this.V[k], 1);
//                    exp.addTerm(this.V[l], -1);
//                    exp.addTerm(this.U[k][l], data.StopsCount - 1);
//                    this.cplex.addLe(exp, data.StopsCount - 2);
//                }
//            }
//    }
//    
//    public int[] Solve(InputData d) throws IloException {
//        boolean condition = this.cplex.solve();
//        if(condition) {
//            System.out.println(this.cplex.getStatus()+" = "+this.cplex.getObjValue());
//            double[][] U = new double[this.U.length][];
//            for (int i = 0;i < this.U.length; i++) {
//                U[i] = this.cplex.getValues(this.U[i]);
//                System.out.println(Arrays.toString(U[i]));
//            }
//            double[] v = this.cplex.getValues(this.V);
//            System.out.println(v);
//            int[] tour = new int[this.U.length];
//            int i = 0, k = 0;
//            tour[k] = i;
//            k++;
//            while (k < U.length)
//                for (int j = 0; j < this.U.length; j++) {
//                    if(U[i][j] == 1){
//                        i = j;
//                        tour[k++] = i;
//                        break;
//                    }
//                }
//            System.out.println(Arrays.toString(tour));
//            this.cplex.end();
//            return tour;
//        }
//        return null;
//    }
//}
