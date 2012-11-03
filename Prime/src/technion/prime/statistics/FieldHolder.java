package technion.prime.statistics;

import java.util.HashMap;
import java.util.Map;

import technion.prime.utils.StringUtils;

class FieldHolder {
	private Map<Field, Object> fields = new HashMap<Field, Object>();
	
	public synchronized void setField(Field field, Object value) {
		if (value.getClass() != field.getType()) {
			throw new IllegalArgumentException(String.format("field \"%s\" expects type %s, got %s",
					field.getTitle(), field.getType().toString(), value.getClass().toString()));
		}
		fields.put(field, value);
	}
	
	public synchronized Object getField(Field field) {
		if (fields.containsKey(field) == false) return field.getDefault();
		return fields.get(field);
	}
	
	public synchronized String getString(Field field) {
		if (field.getType() == String.class) return (String)getField(field);
		if (field.getType() == Integer.class) return StringUtils.prettyPrintNumber((Integer)getField(field));
		if (field.getType() == Long.class) return StringUtils.prettyPrintNumber((Long)getField(field));
		if (field.getType() == Double.class) return StringUtils.prettyPrintNumber((Double)getField(field));
		return getField(field).toString();
	}
	
	public double getDouble(Field f) {
		if (f.getType() == Integer.class) return (Integer)getField(f);
		if (f.getType() == Long.class) return (Long)getField(f);
		if (f.getType() == Double.class) return (Double)getField(f);
		throw new ClassCastException("cannot get double value from field " + f.getTitle() + " of type " + f.getType());
	}
	
	public int getInteger(Field f) {
		if (f.getType() == Integer.class) return (Integer)getField(f);
		throw new ClassCastException("cannot get integer value from field " + f.getTitle() + " of type " + f.getType());
	}
	
	public synchronized void incrementField(Field field) {
		// Currently only implemented for integers
		if (field.getType() != Integer.class) {
			throw new IllegalArgumentException(String.format("field \"%s\" of type %s cannot be incremented",
					field.getTitle(), field.getType().toString()));
		}
		setField(field, ((Integer)getField(field)) + 1); 
	}

	
}
