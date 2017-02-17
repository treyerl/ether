package ch.fhnw.util.math.geometry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ch.fhnw.util.Pair;
import ch.fhnw.util.math.Vec3;

/**Implements Iterable by providing a segment iterator; segments as Pair&lt;Vec3,Vec3&gt;
 * @author treyerl
 *
 */
public class LineString implements Iterable<Pair<Vec3, Vec3>>{
	protected LinkedList<Vec3> points;
	
	protected LineString(){
		points = new LinkedList<>();
	}
	
	public LineString(LinkedList<Vec3> points){
		this.points = points;
	}
	
	/**Creates a copy of all points and duplicates all but the end points.
	 * @return
	 */
	public List<Vec3> getLines(){
		List<Vec3> lines = new LinkedList<>();
		Vec3 first;
		Vec3 last = first = points.removeFirst();
		for (Vec3 point: points){
			lines.add(last);
			lines.add(point);
			last = point;
		}
		points.addFirst(first);
		return lines;
	}
	
	/**Line segment iterator; segments as Pair&lt;Vec3,Vec3&gt;
	 * @return Iterator iterating over line segments
	 */
	public Iterator<Pair<Vec3, Vec3>> getSegments(){
		if (points.size() < 2) throw new IllegalArgumentException("points list to short");
		return new Iterator<Pair<Vec3, Vec3>>(){
			Iterator<Vec3> it = points.iterator();
			Vec3 last = it.next();
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Pair<Vec3, Vec3> next() {
				Vec3 point = it.next();
				Pair<Vec3, Vec3> p = new Pair<>(last, point);
				last = point;
				return p;
			}
		};
	}
	
	public boolean isClosed(){
		return points.getFirst().equals(points.getLast());
	}
	
	public LineString setClosed(){
		if (!isClosed()) points.add(points.getFirst());
		return this;
	}
	
	public LineString setOpen(){
		if (isClosed()) points.removeLast();
		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Pair<Vec3, Vec3>> iterator() {
		return getSegments();
	}
}
