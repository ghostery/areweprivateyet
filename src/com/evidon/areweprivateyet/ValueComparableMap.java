// Copyright 2013 Evidon.  All rights reserved.
// Use of this source code is governed by a Apache License 2.0
// license that can be found in the LICENSE file.

package com.evidon.areweprivateyet;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Functions;
import com.google.common.collect.Ordering;

public class ValueComparableMap<K extends Comparable<K>,V> extends TreeMap<K,V> {
	private static final long serialVersionUID = 5424540773170446486L;

	//A map for doing lookups on the keys for comparison so we don't get infinite loops
    private final Map<K, V> valueMap;

    ValueComparableMap(final Ordering<? super V> partialValueOrdering) {
        this(partialValueOrdering, new HashMap<K,V>());
    }

    private ValueComparableMap(Ordering<? super V> partialValueOrdering, HashMap<K, V> valueMap) {
        super(partialValueOrdering //Apply the value ordering
                .onResultOf(Functions.forMap(valueMap)) //On the result of getting the value for the key from the map
                .compound(Ordering.natural())); //as well as ensuring that the keys don't get clobbered
        this.valueMap = valueMap;
    }

    public V put(K k, V v) {
        if (valueMap.containsKey(k)){
            //remove the key in the sorted set before adding the key again
            remove(k);
        }
        valueMap.put(k,v); //To get "real" unsorted values for the comparator
        return super.put(k, v); //Put it in value order
    }

    public static void main(String[] args){
        TreeMap<String, Integer> map = new ValueComparableMap<String, Integer>(Ordering.natural());
        map.put("a", 5);
        map.put("b", 1);
        map.put("c", 3);
        assertEquals("b",map.firstKey());
        assertEquals("a",map.lastKey());
        map.put("d",0);
        assertEquals("d",map.firstKey());
        //ensure it's still a map (by overwriting a key, but with a new value) 
        map.put("d", 2);
        assertEquals("b", map.firstKey());
        //Ensure multiple values do not clobber keys
        map.put("e", 2);
        assertEquals(5, map.size());
        assertEquals(2, (int) map.get("e"));
        assertEquals(2, (int) map.get("d"));
    }
 }