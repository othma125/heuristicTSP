//
//import ilog.concert.IloException;
//import ilog.concert.IloIntVar;
//import ilog.concert.IloLinearIntExpr;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.cplex.IloCplex;
//import java.io.File;
//import java.util.Arrays;
//import java.util.Vector;
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
//    public CplexModel(InputDataTSP d) throws IloException{
//        this.cplex=new IloCplex();
//        this.U=new IloIntVar[d.SitesCounter][];
//        this.V=new IloIntVar[d.SitesCounter];
//        for(int k=0;k<this.U.length;k++)
//            this.U[k]=this.cplex.boolVarArray(d.SitesCounter);
//        this.V=this.cplex.numVarArray(d.SitesCounter,0,d.SitesCounter-1);
//        this.modelDefinition(d);
//    }
//    public void modelDefinition(InputDataTSP d) throws IloException{
//        this.objective(d);
//        this.contrainte1();
//        this.contrainte2(d);
//    }
//    public void objective(InputDataTSP d) throws IloException{
//        IloNumExpr exp=this.cplex.numExpr();
//        for(int k=0;k<this.U.length;k++)
//            for(int l=0;l<this.U.length;l++)
//                exp=this.cplex.sum(this.cplex.prod(d.CostMatrix[k][l],this.U[k][l]),exp);
//        this.cplex.addMinimize(exp);
//    }
//    public void contrainte1() throws IloException{
//        for(int i=0;i<this.U.length;i++){
//            IloLinearIntExpr exp=this.cplex.linearIntExpr();
//            for(int k=0;k<this.U.length;k++){
//                if(k==i)
//                    continue;
//                exp.addTerm(this.U[i][k],1);
//            }
//            this.cplex.addEq(exp,1);
//        }
//        for(int i=0;i<this.U.length;i++){
//            IloLinearIntExpr exp=this.cplex.linearIntExpr();
//            for(int k=0;k<this.U.length;k++){
//                if(k==i)
//                    continue;
//                exp.addTerm(this.U[k][i],1);
//            }
//            this.cplex.addEq(exp,1);
//        }
//    }
//    public void contrainte2(InputDataTSP d) throws IloException{
//        for(int k=1;k<this.U.length;k++)
//            for(int l=1;l<this.U.length;l++){
//                if(k!=l){
//                    IloLinearNumExpr exp=this.cplex.linearNumExpr();
//                    exp.addTerm(this.V[k],1);
//                    exp.addTerm(this.V[l],-1);
//                    exp.addTerm(this.U[k][l],d.SitesCounter-1);
//                    this.cplex.addLe(exp,d.SitesCounter-2);
//                }
//            }
//    }
//    public void Solve(InputDataTSP d) throws IloException{
//        boolean condition=this.cplex.solve();
//        if(condition){
////        [0, 23, 12, 15, 26, 7, 22, 6, 24, 18, 10, 21, 16, 13, 17, 14, 3, 9, 19, 1, 20, 4, 28, 2, 25, 8, 11, 5, 27]
//            System.out.println(this.cplex.getStatus()+"="+this.cplex.getObjValue());
//            double[][] U=new double[this.U.length][];
//            for(int i=0;i<this.U.length;i++){
//                U[i]=this.cplex.getValues(this.U[i]);
////                System.out.println(Arrays.toString(U[i]));
//            }
////            double[] v=this.cplex.getValues(this.V);
////            System.out.println(v);
//            int[] tg=new int[this.U.length];
//            int i=0,k=0;
//            tg[k]=i;
//            k++;
//            while(k<U.length)
//                for(int j=0;j<this.U.length;j++){
//                    if(U[i][j]==1){
//                        i=j;
//                        tg[k]=i;
//                        k++;
//                        break;
//                    }
//                }
//            System.out.println(Arrays.toString(tg));
////            this.fonction(d,tg);
//            this.cplex.end();
//        }
//    }
//    public static void main(String[] args) throws IloException {
//        // TODO code application logic here
//        InputDataTSP donnee=new InputDataTSP(new File("instances\\29.txt"));
//        new CplexModel(donnee).Solve(donnee);
////        new Tree(donnee,0,null,0d);
//    }
////    public double alpha(InputDataTSP d,int[] tg,Point p1,Point p2,int i,int j){
////        double alpha=p1.VectProd(p2);
////        alpha/=d.CostMatrix[tg[i]][tg[j]];
////        alpha/=d.CostMatrix[tg[i]][tg[i-1]];
//////        alpha=Math.abs(alpha);
//////        alpha=Math.asin(alpha);
////        return alpha;
////    }
////    public void fonction(InputDataTSP d,int[] tg){
////        for(int i=1;i<tg.length-1;i++){
////            Point p2,p1=new Point(d.Coordinates.elementAt(tg[i]).abscisse-d.Coordinates.elementAt(tg[i-1]).abscisse,
////                    d.Coordinates.elementAt(tg[i]).ordonnee-d.Coordinates.elementAt(tg[i-1]).ordonnee);
////            for(int j=i+1;j<tg.length;j++){
////                p2=new Point(d.Coordinates.elementAt(tg[j]).abscisse-d.Coordinates.elementAt(tg[i]).abscisse,
////                        d.Coordinates.elementAt(tg[j]).ordonnee-d.Coordinates.elementAt(tg[i]).ordonnee);
////                System.out.println(tg[i]+" "+tg[j]+" "+this.alpha(d,tg,p1,p2,i,j)+" "+d.CostMatrix[tg[i]][tg[j]]);
////            }
////            System.out.println(Arrays.toString(tg));
////            System.out.println();
////        }
////    }
//}