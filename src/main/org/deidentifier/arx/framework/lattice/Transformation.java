/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.framework.lattice;

import java.util.Arrays;
import java.util.Iterator;

import org.deidentifier.arx.framework.check.NodeChecker;
import org.deidentifier.arx.metric.InformationLoss;

import com.carrotsearch.hppc.LongArrayList;

import de.linearbits.jhpl.Lattice;
import de.linearbits.jhpl.PredictiveProperty;
import de.linearbits.jhpl.PredictiveProperty.Direction;

/**
 * The class Transformation.
 * 
 * @author Fabian Prasser
 */
public class Transformation {

    /** The id. */
    private final long                      id;

    /** The lattice */
    private final Lattice<Integer, Integer> lattice;

    /** The level */
    private final int                       level;

    /** The solution space */
    private final SolutionSpace             solutionSpace;

    /** Transformation in ARX's space */
    private final int[]                     transformationARX;

    /** Transformation in JHPL's space */
    private final int[]                     transformationJHPL;

    /**
     * Instantiates a new transformation.
     * @param transformation In ARX space
     * @param lattice
     * @param solutionSpace 
     */
    public Transformation(int[] transformation, Lattice<Integer, Integer> lattice, SolutionSpace solutionSpace) {
        this.lattice = lattice;
        this.solutionSpace = solutionSpace;
        this.transformationARX = transformation;
        this.transformationJHPL = solutionSpace.toJHPL(transformation);
        this.level = getLevel(transformationARX);
        this.id = lattice.space().toId(transformationJHPL);
        
    }

    /**
     * Returns associated data
     * @return
     */
    public Object getData() {
        return this.solutionSpace.getData(this.id);
    }

    /**
     * Returns the generalization
     * @return
     */
    public int[] getGeneralization() {
        return this.transformationARX;
    }
    
    /**
     * Returns the id
     * @return
     */
    public long getIdentifier() {
        return id;
    }

    /**
     * Returns the information loss
     * @return
     */
    public InformationLoss<?> getInformationLoss() {
        return solutionSpace.getInformationLoss(this.id);
    }

    /**
     * Return level
     * @return
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Returns the lower bound on information loss
     * @return
     */
    public InformationLoss<?> getLowerBound() {
        return solutionSpace.getLowerBound(this.id);
    }

    /**
     * Returns whether this transformation has a given property
     * @param property
     * @return
     */
    public boolean hasProperty(PredictiveProperty property) {
        return this.lattice.hasProperty(this.transformationJHPL, property);
    }

    /**
     * Sets the properties to the given node.
     *
     * @param node the node
     * @param result the result
     */
    public void setChecked(NodeChecker.Result result) {
        
        // Set checked
        this.setProperty(solutionSpace.getPropertyChecked());
        
        // Anonymous
        if (result.privacyModelFulfilled){
            this.setProperty(solutionSpace.getPropertyAnonymous());
        } else {
            this.setProperty(solutionSpace.getPropertyNotAnonymous());
        }

        // k-Anonymous
        if (result.minimalClassSizeFulfilled != null) {
            if (result.minimalClassSizeFulfilled){
                this.setProperty(solutionSpace.getPropertyKAnonymous());
            } else {
                this.setProperty(solutionSpace.getPropertyNotKAnonymous());
            }
        }

        // Infoloss
        this.setInformationLoss(result.informationLoss);
        this.setLowerBound(result.lowerBound);
    }

    /**
     * Sets a data
     * @param object
     */
    public void setData(Object object) {
        this.solutionSpace.setData(this.id, object);
    }

    /**
     * Sets the information loss
     * @param informationLoss
     */
    public void setInformationLoss(InformationLoss<?> informationLoss) {
        this.solutionSpace.setInformationLoss(this.id, informationLoss);
    }

    /**
     * Sets the lower bound
     * @param lowerBound
     */
    public void setLowerBound(InformationLoss<?> lowerBound) {
        this.solutionSpace.setLowerBound(this.id, lowerBound);
    }

    /**
     * Sets a property
     * @param property
     */
    public void setProperty(PredictiveProperty property) {
        this.lattice.putProperty(this.transformationJHPL, property);
    }

    /**
     * Sets the property to all neighbors
     * @param property
     */
    public void setPropertyToNeighbours(PredictiveProperty property) {
        Iterator<Long> neighbors;
        if (property.getDirection() == Direction.UP) {
            neighbors = lattice.space().indexIteratorToIdIterator(lattice.nodes().listSuccessors(transformationJHPL));
        } else if (property.getDirection() == Direction.DOWN) {
            neighbors = lattice.space().indexIteratorToIdIterator(lattice.nodes().listPredecessors(transformationJHPL));
        } else {
            return;
        }
        LongArrayList list = new LongArrayList();
        for (;neighbors.hasNext();) {
            list.add(neighbors.next());
        }
        for (long id : list.toArray()) {
            lattice.putProperty(lattice.space().toIndex(id), property);
        }
    }
    

    /**
     * Returns a string representation
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Transformation {\n");
        builder.append(" - Solution space: ").append(this.solutionSpace.hashCode()).append("\n");
        builder.append(" - Index: ").append(Arrays.toString(transformationJHPL)).append("\n");
        builder.append(" - Id: ").append(id).append("\n");
        builder.append(" - Generalization: ").append(Arrays.toString(transformationARX)).append("\n");
        builder.append(" - Level: ").append(level).append("\n");
        builder.append(" - Properties:\n");
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyAnonymous())) {
            builder.append("   * ANONYMOUS: ").append(solutionSpace.getPropertyAnonymous().getDirection()).append("\n");    
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyNotAnonymous())) {
            builder.append("   * NOT_ANONYMOUS: ").append(solutionSpace.getPropertyNotAnonymous().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyKAnonymous())) {
            builder.append("   * K_ANONYMOUS: ").append(solutionSpace.getPropertyKAnonymous().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyNotKAnonymous())) {
            builder.append("   * NOT_K_ANONYMOUS: ").append(solutionSpace.getPropertyNotKAnonymous().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyChecked())) {
            builder.append("   * CHECKED: ").append(solutionSpace.getPropertyChecked().getDirection()).append("\n");    
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyForceSnapshot())) {
            builder.append("   * FORCE_SNAPSHOT: ").append(solutionSpace.getPropertyForceSnapshot().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyInsufficientUtility())) {
            builder.append("   * INSUFFICIENT_UTILITY: ").append(solutionSpace.getPropertyInsufficientUtility().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertySuccessorsPruned())) {
            builder.append("   * SUCCESSORS_PRUNED: ").append(solutionSpace.getPropertySuccessorsPruned().getDirection()).append("\n");
        }
        if (lattice.hasProperty(transformationJHPL, solutionSpace.getPropertyVisited())) {
            builder.append("   * VISITED: ").append(solutionSpace.getPropertyVisited().getDirection()).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Returns the sum of all transformation levels;
     * @param transformation
     * @return
     */
    private int getLevel(int[] transformation) {
        int level = 0; for (int lvl : transformation) level += lvl;
        return level;
    }
}