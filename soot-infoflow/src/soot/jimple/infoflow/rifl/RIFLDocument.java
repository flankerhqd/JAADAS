/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.rifl;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a full RIFL specification document
 * 
 * @author Steven Arzt
 */
public class RIFLDocument {

	private AttackerIO attackerIO = new AttackerIO();
	private List<DomainSpec> domains = new ArrayList<DomainSpec>();
	private List<SourceSinkDomPair> domainAssignment = new ArrayList<SourceSinkDomPair>();
	private List<DomPair> domainHierarchy = new ArrayList<DomPair>();
	private List<FlowPair> flowPolicy = new ArrayList<FlowPair>();
	
	private TopDomain topDomain = null;
	private BottomDomain bottomDomain = null;
	
	/**
	 * The source and sink specification in a RIFL document (the IO channels
	 * through which an attacker can communicate with an application)
	 */
	public class AttackerIO {
		private List<SourceSinkSpec> sources = new ArrayList<SourceSinkSpec>();
		private List<SourceSinkSpec> sinks = new ArrayList<SourceSinkSpec>();
		
		/**
		 * Gets the list of sources defined in this attacker IO specification
		 * @return The list of sources defined in this attacker IO specification
		 */
		public List<SourceSinkSpec> getSources() {
			return this.sources;
		}

		/**
		 * Gets the list of sinks defined in this attacker IO specification
		 * @return The list of sinks defined in this attacker IO specification
		 */
		public List<SourceSinkSpec> getSinks() {
			return this.sinks;
		}
		
		@Override
		public int hashCode() {
			return 31 * this.sources.hashCode()
					+ 31 * this.sinks.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof AttackerIO))
				return false;
			AttackerIO otherIO = (AttackerIO) other;
			return this.sources.equals(otherIO.sources)
					&& this.sinks.equals(otherIO.sinks);
		}
	}
	
	/**
	 * Abstract base class for all source and sink specifications in RIFL
	 */
	public abstract class SourceSinkSpec {
		
	}
	
	/**
	 * Instance of the {@link SourceSinkSpec} class for Java 
	 */
	public class JavaSourceSinkSpec extends SourceSinkSpec {
		
	}
	
	/**
	 * Class that models a method parameter in Java specified as a source or
	 * sink in RIFL
	 */
	public class JavaParameterSpec extends JavaSourceSinkSpec {
		private final String packageName;
		private final String className;
		private final String halfSignature;
		private final int paramIdx;
		
		/**
		 * Creates a new instance of the {@link JavaParameterSpec} class
		 * @param packageName The name of the Java package containing the class
		 * which contains the method whose parameter is being configured as a
		 * source or sink.
		 * @param className The name of the class containing the parameter to
		 * be defined as a source or sink.
		 * @param halfSignature The method name and the formal parameters with
		 * fully-qualified names in brackets. This is like a Soot subsignature,
		 * except for the return type which is omitted.
		 * @param paramIdx The index of the parameter to be defined as a source
		 * or sink. 0 refers to the return value, so the list is 1-based.
		 */
		public JavaParameterSpec(String packageName, String className,
				String halfSignature, int paramIdx) {
			this.packageName = packageName;
			this.className = className;
			this.halfSignature = halfSignature;
			this.paramIdx = paramIdx;
		}
		
		/**
		 * Gets the name of the Java package containing the class which contains
		 * the method whose parameter is being configured as a source or sink.
		 * @return The package name
		 */
		public String getPackageName() {
			return this.packageName;
		}
		
		/**
		 * Gets the name of the class containing the parameter to be defined as
		 * a source or sink.
		 * @return The class name
		 */
		public String getClassName() {
			return this.className;
		}
		
		/**
		 * Gets the method name and the formal parameters with fully-qualified
		 * names in brackets. This is like a Soot subsignature, except for the
		 * return type which is omitted.
		 * @return The method name and the method's fully qualified parameter
		 * list
		 */
		public String getHalfSignature() {
			return this.halfSignature;
		}
		
		/**
		 * Gets the index of the parameter to be defined as a source or sink. 0
		 * refers to the return value, so the list is 1-based.
		 * @return The parameter index of the tainted parameter
		 */
		public int getParamIdx() {
			return this.paramIdx;
		}

		@Override
		public int hashCode() {
			return 31 * this.packageName.hashCode()
					+ 31 * this.className.hashCode()
					+ 31 * this.halfSignature.hashCode()
					+ 31 * this.paramIdx;
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof JavaParameterSpec))
				return false;
			JavaParameterSpec otherSpec = (JavaParameterSpec) other;
			return this.packageName.equals(otherSpec.packageName)
					&& this.className.equals(otherSpec.className)
					&& this.halfSignature.equals(otherSpec.halfSignature)
					&& this.paramIdx == otherSpec.paramIdx;
		}
	}
	
	/**
	 * Class that models a static field in Java specified as a source or sink
	 * in RIFL
	 */
	public class JavaFieldSpec extends JavaSourceSinkSpec {
		private final String packageName;
		private final String className;
		private final String fieldName;
		
		/**
		 * Creates a new instance of the {@link JavaFieldSpec} class
		 * @param packageName The name of the Java package containing the class
		 * with the static field to be considered as a source or sink
		 * @param className The name of the class containing the static field to
		 * be defined as a source or sink.
		 * @param fieldName The name of the static field to be treated as a source
		 * or sink
		 */
		public JavaFieldSpec(String packageName, String className, String fieldName) {
			this.packageName = packageName;
			this.className = className;
			this.fieldName = fieldName;
		}
		
		/**
		 * Gets the name of the Java package containing the class with the static
		 * field to be considered as a source or sink
		 * @return The package name
		 */
		public String getPackageName() {
			return this.packageName;
		}
		
		/**
		 * Gets the name of the class containing the static field to be defined as
		 * a source or sink.
		 * @return The class name
		 */
		public String getClassName() {
			return this.className;
		}
		
		/**
		 * Gets the name of the static field to be treated as a source or sink
		 * @return The name of the static field to be treated as a source or sink
		 */
		public String getFieldName() {
			return this.fieldName;
		}

		@Override
		public int hashCode() {
			return 31 * this.packageName.hashCode()
					+ 31 * this.className.hashCode()
					+ 31 * this.fieldName.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof JavaFieldSpec))
				return false;
			JavaFieldSpec otherSpec = (JavaFieldSpec) other;
			return this.packageName.equals(otherSpec.packageName)
					&& this.className.equals(otherSpec.className)
					&& this.fieldName.equals(otherSpec.fieldName);
		}
	}

	/**
	 * Class representing a domain in the RIFL specification
	 */
	public abstract class DomainSpec {
	}
	
	/**
	 * The fixed "TOP" domain
	 */
	public class TopDomain extends DomainSpec {
	}
	
	/**
	 * The fixed "BOTTOM" domain
	 */
	public class BottomDomain extends DomainSpec {
	}
	
	/**
	 * A category based on a named domain
	 */
	public class Category extends DomainSpec {
		private String value = "";
		
		/**
		 * Creates a new instance of the {@link Category} class
		 * @param value The name of the category
		 */
		public Category(String value) {
			this.value = value;
		}
		
		/**
		 * Gets the name of this category
		 * @return The name of this category
		 */
		public String getValue() {
			return this.value;
		}

		@Override
		public int hashCode() {
			return 31 * this.value.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof Category))
				return false;
			Category otherSpec = (Category) other;
			return this.value.equals(otherSpec.value);
		}
	}
	
	/**
	 * Enumeration defining whether a domain is associated with a source or a
	 * sink
	 */
	public enum DomPairType {
		/**
		 * A source is associated with a domain
		 */
		SourceDomPair,
		/**
		 * A sink is associated with a domain
		 */
		SinkDomPair
	}
	
	/**
	 * A source- or sink-domain pair
	 */
	public class SourceSinkDomPair {
		private final SourceSinkSpec sourceOrSink;
		private final DomainSpec domain;
		private final DomPairType type;
		
		/**
		 * Creates a new instance of the {@link SourceSinkDomPair} class
		 * @param sourceOrSink The source or sink to be associated with a domain
		 * @param domain The domain to associate the source or sink with
		 */
		public SourceSinkDomPair(SourceSinkSpec sourceOrSink, DomainSpec domain,
				DomPairType type) {
			this.sourceOrSink = sourceOrSink;
			this.domain = domain;
			this.type = type;
		}
		
		/**
		 * Gets the source or sink associated with a domain
		 * @return The source or sink associated with a domain
		 */
		public SourceSinkSpec getSourceOrSink() {
			return this.sourceOrSink;
		}
		
		/**
		 * Gets the domain the source or sink is associated with
		 * @return The domain the source or sink is associated with
		 */
		public DomainSpec getDomain() {
			return this.domain;
		}
		
		/**
		 * Gets whether a source or a sink is associated with a domain in this
		 * object
		 * @return The type of the association
		 */
		public DomPairType getType() {
			return this.type;
		}

		@Override
		public int hashCode() {
			return 31 * this.sourceOrSink.hashCode()
					+ 31 * this.domain.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof SourceSinkDomPair))
				return false;
			SourceSinkDomPair otherPair = (SourceSinkDomPair) other;
			return this.sourceOrSink.equals(otherPair.sourceOrSink)
					&& this.domain.equals(otherPair.domain);
		}
	}
	
	/**
	 * A pair of domains to be used in the domain hierarchy
	 */
	public class DomPair {
		private final DomainSpec firstDomain;
		private final DomainSpec secondDomain;
		
		/**
		 * Creates a new instance of the {@link DomPair} class
		 * @param firstDomain The first domain in the pair
		 * @param secondDomain The second domain in the pair
		 */
		public DomPair(DomainSpec firstDomain, DomainSpec secondDomain) {
			this.firstDomain = firstDomain;
			this.secondDomain = secondDomain;
		}
		
		/**
		 * Gets the first domain in the pair
		 * @return The first domain in the pair
		 */
		public DomainSpec getFirstDomain() {
			return this.firstDomain;
		}

		/**
		 * Gets the second domain in the pair
		 * @return The second domain in the pair
		 */
		public DomainSpec getSecondDomain() {
			return this.secondDomain;
		}

		@Override
		public int hashCode() {
			return 31 * this.firstDomain.hashCode()
					+ 31 * this.secondDomain.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof DomPair))
				return false;
			DomPair otherPair = (DomPair) other;
			return this.firstDomain.equals(otherPair.firstDomain)
					&& this.secondDomain.equals(otherPair.secondDomain);
		}
	}
	
	/**
	 * Class representing a pair of domains between which information flow is
	 * allowed
	 */
	public class FlowPair {
		private final DomainSpec firstDomain;
		private final DomainSpec secondDomain;
		
		/**
		 * Creates a new instance of the {@link FlowPair} class
		 * @param firstDomain The first domain in the pair, i.e. the start
		 * domain of the information flow
		 * @param secondDomain The second domain in the pair, i.e. the target
		 * domain of the information flow
		 */
		public FlowPair(DomainSpec firstDomain, DomainSpec secondDomain) {
			this.firstDomain = firstDomain;
			this.secondDomain = secondDomain;
		}
		
		/**
		 * Gets the first domain in the pair, i.e. the start domain of the
		 * information flow
		 * @return The first domain in the pair
		 */
		public DomainSpec getFirstDomain() {
			return this.firstDomain;
		}

		/**
		 * Gets the second domain in the pair, i.e. the target domain of the
		 * information flow
		 * @return The second domain in the pair
		 */
		public DomainSpec getSecondDomain() {
			return this.secondDomain;
		}
		
		@Override
		public int hashCode() {
			return 31 * this.firstDomain.hashCode()
					+ 31 * this.secondDomain.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null || !(other instanceof FlowPair))
				return false;
			FlowPair otherPair = (FlowPair) other;
			return this.firstDomain.equals(otherPair.firstDomain)
					&& this.secondDomain.equals(otherPair.secondDomain);
		}
	}

	/**
	 * Gets the attacker IO specification
	 * @return The attacker IO specification
	 */
	public AttackerIO getAttackerIO() {
		return this.attackerIO;
	}
	
	/**
	 * Gets the list of domains
	 * @return The list of domains
	 */
	public List<DomainSpec> getDomains() {
		return this.domains;
	}
	
	/**
	 * Gets the list of domain assignments
	 * @return The list of domain assignments
	 */
	public List<SourceSinkDomPair> getDomainAssignment() {
		return this.domainAssignment;
	}
	
	/**
	 * Gets the domain hierarchy tree
	 * @return The domain hierarchy tree
	 */
	public List<DomPair> getDomainHierarchy() {
		return this.domainHierarchy;
	}
	
	/**
	 * Gets the flow policy as a list of pair of domains between each data
	 * flows are allowed
	 * @return The flow policy
	 */
	public List<FlowPair> getFlowPolicy() {
		return this.flowPolicy;
	}
	
	/**
	 * Gets the version number of the RIFL specification modeled by these data
	 * classes
	 * @return The version number of the RIFL specification
	 */
	public static String getRIFLSpecVersion() {
		return "1.0";
	}
	
	public TopDomain getTopDomain() {
		if (topDomain == null)
			topDomain = new TopDomain();
		return topDomain;
	}

	public BottomDomain getBottomDomain() {
		if (bottomDomain == null)
			bottomDomain = new BottomDomain();
		return bottomDomain;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attackerIO == null) ? 0 : attackerIO.hashCode());
		result = prime
				* result
				+ ((domainAssignment == null) ? 0 : domainAssignment.hashCode());
		result = prime * result
				+ ((domainHierarchy == null) ? 0 : domainHierarchy.hashCode());
		result = prime * result + ((domains == null) ? 0 : domains.hashCode());
		result = prime * result
				+ ((flowPolicy == null) ? 0 : flowPolicy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof RIFLDocument))
			return false;
		RIFLDocument other = (RIFLDocument) obj;
		return attackerIO.equals(other.attackerIO)
				&& domainAssignment.equals(other.domainAssignment)
				&& domainHierarchy.equals(other.domainHierarchy)
				&& domains.equals(other.domains)
				&& flowPolicy.equals(other.flowPolicy);
	}

}
