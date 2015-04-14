package cs276.util;

import java.util.*;


/**
 * A generic-typed pair of objects.
 * 
 * @author Dan Klein
 */
public class Pair<F, S> implements Comparator<Pair<F, S>>, Comparable<Pair<F, S>>{
	private F first;
	private S second;

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public void setFirst(F val) {
		this.first = val;
	}

	public void setSecond(S val) {
		this.second = val;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Pair))
			return false;

		@SuppressWarnings("unchecked")
		final Pair<F, S> pair = (Pair<F, S>) o;

		if (first != null ? !first.equals(pair.first) : pair.first != null)
			return false;
		if (second != null ? !second.equals(pair.second) : pair.second != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (first != null ? first.hashCode() : 0);
		result = 29 * result + (second != null ? second.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "(" + getFirst() + ", " + getSecond() + ")";
	}

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public static <E, F> Pair<E, F> make(E car, F cdr) {
		return new Pair<E, F>(car, cdr);
	}

	// Overriding the compareTo method
	// Assumes F and S are both Integers
	public int compareTo(Pair<F,S> pr){
		if(!(pr.getFirst() instanceof Integer) || !(pr.getSecond() instanceof Integer)
			|| !(this.getFirst() instanceof Integer) || !(this.getSecond() instanceof Integer)){
			return 0;
		}
		if(((Integer)this.getFirst()).compareTo(((Integer)pr.getFirst())) != 0){
			return ((Integer)this.getFirst()).compareTo(((Integer)pr.getFirst()));
		}else{
			return ((Integer)this.getSecond()).compareTo(((Integer)pr.getSecond()));
		}
	}

	// Overriding the compare method
	// Assumes F and S are both Integers
	public int compare(Pair<F,S> pr1, Pair<F,S> pr2){
		if(!(pr1.getFirst() instanceof Integer) || !(pr1.getSecond() instanceof Integer)
			|| !(pr2.getFirst() instanceof Integer) || !(pr2.getSecond() instanceof Integer)){
			return 0;
		}
		if((Integer)pr1.getFirst() - (Integer)pr2.getFirst() != 0){
			return (Integer)pr1.getFirst() - (Integer)pr2.getFirst();
		}else{
			return (Integer)pr1.getSecond() - (Integer)pr2.getSecond();
		}
	}
}
