package technion.prime.history.edgeset;

import technion.prime.dom.UnknownAppType;
import technion.prime.dom.UnknownMethod;
import technion.prime.dom.UnknownMethodFromField;
import technion.prime.dom.UnknownMethodFromParam;

import technion.prime.dom.AppMethodRef;
import technion.prime.dom.dummy.DummyAppType;
import technion.prime.dom.AppType;
import technion.prime.dom.dummy.DummyAppMethodRef;
import technion.prime.Options;

import java.util.HashMap;
import java.util.Map;


public class EdgeHistoryBuilder {
	public class EdgeBuilder {
		private EdgeNode from;
		private EdgeNode to;
		private AppType returnType;
		private AppType[] paramTypes;
		private Double weight;
		private String methodName;
		private AppType definingType;
		private UnknownType unknownType;
		
		public EdgeBuilder from(String node) {
			if (from != null) throw new IllegalStateException("'from' node already defined");
			from = getNodeFromName(node);
			return this;
		}
		private EdgeNode getFrom() {
			if (from == null) throw new IllegalStateException("no 'from' node defined");
			return from;
		}
		
		public EdgeBuilder to(String node) {
			if (to != null) throw new IllegalStateException("'to' node already defined");
			to = getNodeFromName(node);
			return this;
		}
		private EdgeNode getTo() {
			if (to == null) throw new IllegalStateException("no 'to' node defined");
			return to;
		}
		
		public EdgeBuilder fromRoot() {
			return from(ROOT_NAME);
		}
		
		public EdgeBuilder returning(String type) {
			if (returnType != null) throw new IllegalStateException("return type already defined");
			returnType = createType(type);
			return this;
		}
		private AppType getReturnType() {
			return returnType == null ? VOID_TYPE : returnType;
		}
		
		public EdgeBuilder returningUnknown() {
			if (returnType != null) throw new IllegalStateException("return type already defined");
			returnType = UNKNOWN_TYPE;
			return this;
		}
		
		public EdgeBuilder params(String... paramTypeNames) {
			if (paramTypes != null) throw new IllegalStateException("parameter types already defined");
			paramTypes = new AppType[paramTypeNames.length];
			for (int i = 0; i < paramTypeNames.length; i++) {
				paramTypes[i] = createType(paramTypeNames[i]);
			}
			return this;
		}
		private AppType[] getParamTypes() {
			return paramTypes == null ? new AppType[0] : paramTypes;
		}
		
		public EdgeBuilder weight(double weight) {
			if (this.weight != null) throw new IllegalStateException("weight already defined");
			this.weight = weight;
			return this;
		}
		private double getWeight() {
			return weight == null ? 1 : weight;
		}
		
		public EdgeBuilder name(String name) {
			if (this.methodName != null) throw new IllegalStateException("method name already defined");
			methodName = name;
			return this;
		}
		
		public EdgeBuilder definedBy(String type) {
			if (definingType != null) throw new IllegalStateException("defining type already defined");
			definingType = createType(type);
			return this;
		}
		private AppType getDefiningType() {
			return definingType == null ? BLAND_TYPE : definingType;
		}
		
		public EdgeBuilder unknownType(UnknownType t) {
			if (unknownType != null) throw new IllegalStateException("unknown type already defined");
			unknownType = t;
			return this;
		}
		private UnknownType getUnknownType() {
			return unknownType == null ? UnknownType.FROM_METHOD : unknownType;
		}
		
		public EdgeHistoryBuilder buildEdge() {
			Edge newEdge = new Edge(getFrom(), getTo(), createMethod(), getWeight());
			h.addEdge(newEdge);
			sequence.addLast(newEdge);
			return EdgeHistoryBuilder.this;
		}
		
		private AppMethodRef createMethod() {
			if (methodName != null) {
				return new DummyAppMethodRef(
						getDefiningType(),
						getReturnType(),
						methodName,
						getParamTypes());
			}
			switch (getUnknownType()) {
			case FROM_METHOD: return new UnknownMethod(getDefiningType(), null);
			case FROM_FIELD: return new UnknownMethodFromField(getDefiningType(), null);
			case FROM_PARAMETER: return new UnknownMethodFromParam(getDefiningType());
			}
			throw new AssertionError("could not handle unknown type");
		}
	}
	
	public enum UnknownType {
		FROM_FIELD,
		FROM_PARAMETER,
		FROM_METHOD,
	}
	
	private static final String ROOT_NAME = "root";
	private final AppType VOID_TYPE = createType("void");
	private final AppType BLAND_TYPE = createType("T");
	private final AppType UNKNOWN_TYPE = new UnknownAppType();
	private final EdgeHistory h;
	private final Map<String, EdgeNode> nodeNames = new HashMap<String, EdgeNode>();
	private final EdgeSequence sequence = new EdgeSequence();
	
	public EdgeHistoryBuilder(Options options) {
		h = new EdgeHistory(options);
	}
	
	public EdgeBuilder withEdge() {
		return new EdgeBuilder();
	}
	
	private AppType createType(String typeName) {
		if (typeName.equals(UNKNOWN_TYPE)) return new UnknownAppType();
		return new DummyAppType(typeName);
	}

	public EdgeHistory buildHistory() {
		return h;
	}
	
	public EdgeSequence buildSequence() {
		return sequence;
	}

	private EdgeNode getNodeFromName(String name) {
		if (nodeNames.containsKey(name) == false) {
			EdgeNode n = null;
			if (name.equals(ROOT_NAME)) {
				n = h.getRoot();
			} else {
				n = new EdgeNode();
				h.addNode(n);
			}
			nodeNames.put(name, n);
		}
		return nodeNames.get(name);
	}
}
