/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HeuristicApproach.LSM;

import Data.InputData;

/**
 *
 * @author Othmane
 */
public abstract class LocalSearchMove {
    
    final int[] Sequence;
    double Gain = 0d;
    final int I, J;

    public LocalSearchMove(int[] sequence, int i, int j) {
        this.Sequence = sequence;
        this.I = i;
        this.J = j;
    }

    public double getGain() {
        return this.Gain;
    }

    public double getGain(InputData data) {
        this.setGain(data);
        return this.Gain;
    }
    
    public abstract void Perform(int[] sequence);
    
    public abstract void setGain(InputData data);
}
