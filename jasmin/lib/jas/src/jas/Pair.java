package jas;

public class Pair<T, U>
{
	protected T o1;
	protected U o2;
	public Pair() { o1 = null; o2 = null; }
    public Pair( T o1, U o2 ) { this.o1 = o1; this.o2 = o2; }
    public int hashCode() {
        return o1.hashCode() + o2.hashCode();
    }
    public boolean equals( Object other ) {
        if( other instanceof Pair) {
            Pair p = (Pair) other;
            return o1.equals( p.o1 ) && o2.equals( p.o2 );
        } else return false;
    }

    public String toString() {
        return "<"+o1+","+o2+">";
    }
    public T getO1() { return o1; }
    public U getO2() { return o2; }
}