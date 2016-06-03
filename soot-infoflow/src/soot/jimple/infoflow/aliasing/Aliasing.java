package soot.jimple.infoflow.aliasing;

import heros.solver.IDESolver;

import java.util.Collection;

import soot.Local;
import soot.RefLikeType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.BasePair;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.toolkits.graph.UnitGraph;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Helper class for aliasing operations
 * 
 * @author Steven Arzt
 */
public class Aliasing {
	
	private final IAliasingStrategy aliasingStrategy;
	private final IInfoflowCFG cfg;
	
	protected final LoadingCache<SootMethod,LocalMustAliasAnalysis> strongAliasAnalysis =
			IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,LocalMustAliasAnalysis>() {
				@Override
				public LocalMustAliasAnalysis load(SootMethod method) throws Exception {
					return new StrongLocalMustAliasAnalysis
							((UnitGraph) cfg.getOrCreateUnitGraph(method));
				}
			});
	
	public Aliasing(IAliasingStrategy aliasingStrategy, IInfoflowCFG cfg) {
		this.aliasingStrategy = aliasingStrategy;
		this.cfg = cfg;
	}
	
	/**
	 * Gets whether an access path can point to the same runtime object as another
	 * or to an object reachable through the other
	 * @param taintedAP The access path that is tainted
	 * @param referencedAP The access path that is accessed
	 * @return The access path that actually matched if the access paths alias.
	 * In the simplest case, this is the given tainted access path.
	 * When using recursive access paths, it can however also be a base
	 * expansion. If the given access paths do not alias, null is returned.
	 */
	public AccessPath mayAlias(AccessPath taintedAP, AccessPath referencedAP) {
		// Check whether the access paths are directly equal
		if (taintedAP.equals(referencedAP))
			return taintedAP;
		
		// Ask an interactive aliasing strategy if we have one
		// TODO
		/*
		if (aliasingStrategy.isInteractive())
			return aliasingStrategy.mayAlias(taintedAP, referencedAP);
		*/
		
		if (taintedAP.isInstanceFieldRef() || taintedAP.isLocal()) {
			// For instance field references, the base must match
			if (taintedAP.getPlainValue() != referencedAP.getPlainValue())
				return null;
			
			// Shortcut: If we have no fields and the base matches, we're done
			if (referencedAP.getFieldCount() == 0)
				return taintedAP;
			
			// If the referenced AP is not an instance field reference, we're done
			if (!referencedAP.isInstanceFieldRef())
				return null;
		}
		
		// If one reference is static, the other one must be static as well
		if (taintedAP.isStaticFieldRef())
			if (!referencedAP.isStaticFieldRef())
				return null;
		
		// Match the bases
		return getReferencedAPBase(taintedAP, referencedAP.getFields());
	}
	
	/**
	 * Matches the given access path against the given array of fields
	 * @param taintedAP The tainted access paths
	 * @param referencedFields The array of referenced access paths
	 * @return The actually matched access path if a matching was possible,
	 * otherwise null
	 */
	private AccessPath getReferencedAPBase(AccessPath taintedAP,
			SootField[] referencedFields) {
		final Collection<BasePair> bases = taintedAP.isStaticFieldRef()
				? AccessPath.getBaseForType(taintedAP.getFirstFieldType())
						: AccessPath.getBaseForType(taintedAP.getBaseType());
		
		int fieldIdx = 0;
		while (fieldIdx < referencedFields.length) {
			// If we reference a.b.c, this only matches a.b.*, but not a.b
			if (fieldIdx >= taintedAP.getFieldCount()) {
				if (taintedAP.getTaintSubFields())
					return taintedAP;
				else
					return null;
			}
			
			// a.b does not match a.c
			if (taintedAP.getFields()[fieldIdx] != referencedFields[fieldIdx]) {
				// If the referenced field is a base, we add it in. Note that
				// the first field in a static reference is the base, so this
				// must be excluded from base matching.
				if (bases != null && !(taintedAP.isStaticFieldRef() && fieldIdx == 0)) {
					// Check the base. Handles A.y (taint) ~ A.[x].y (ref)
					for (BasePair base : bases) {
						if (base.getFields()[0] == referencedFields[fieldIdx]) {
							// Build the access path against which we have
							// actually matched
							SootField[] cutFields = new SootField
									[taintedAP.getFieldCount() + base.getFields().length];
							Type[] cutFieldTypes = new Type[cutFields.length];
							
							System.arraycopy(taintedAP.getFields(), 0, cutFields, 0, fieldIdx);
							System.arraycopy(base.getFields(), 0, cutFields, fieldIdx, base.getFields().length);
							System.arraycopy(taintedAP.getFields(), fieldIdx, cutFields,
									fieldIdx + base.getFields().length, taintedAP.getFieldCount() - fieldIdx);
							
							System.arraycopy(taintedAP.getFieldTypes(), 0, cutFieldTypes, 0, fieldIdx);
							System.arraycopy(base.getTypes(), 0, cutFieldTypes, fieldIdx, base.getTypes().length);
							System.arraycopy(taintedAP.getFieldTypes(), fieldIdx, cutFieldTypes,
									fieldIdx + base.getTypes().length, taintedAP.getFieldCount() - fieldIdx);

							return new AccessPath(taintedAP.getPlainValue(),
									cutFields, taintedAP.getBaseType(), cutFieldTypes,
									taintedAP.getTaintSubFields(), false, false);
						}
					}
					
				}
				return null;
			}
			
			fieldIdx++;
		}
		
		return taintedAP;
	}

	/**
	 * Gets whether two values may potentially point to the same runtime object
	 * @param val1 The first value
	 * @param val2 The second value
	 * @return True if the two values may potentially point to the same runtime
	 * object, otherwise false
	 */
	public boolean mayAlias(Value val1, Value val2) {
		// What cannot be represented in an access path cannot alias
		if (!AccessPath.canContainValue(val1) || !AccessPath.canContainValue(val2))
			return false;
		
		// Constants can never alias
		if (val1 instanceof Constant || val2 instanceof Constant)
			return false;
		
		// If the two values are equal, they alias by definition
		if (val1 == val2)
			return true;
		
		// If we have an interactive aliasing algorithm, we check that as well
		if (aliasingStrategy.isInteractive())
			return aliasingStrategy.mayAlias(new AccessPath(val1, false), new AccessPath(val2, false));
		
		return false;		
	}
	
	/**
	 * Gets whether a value and an access path may potentially point to the same
	 * runtime object
	 * @param ap The access path
	 * @param val The value
	 * @return The access path that actually matched if the given value and
	 * access path alias. In the simplest case, this is the given access path.
	 * When using recursive access paths, it can however also be a base
	 * expansion. If the given access path and value do not alias, null is
	 * returned.
	 */
	public AccessPath mayAlias(AccessPath ap, Value val) {
		// What cannot be represented in an access path cannot alias
		if (!AccessPath.canContainValue(val))
			return null;
		
		// Constants can never alias
		if (val instanceof Constant)
			return null;
		
		// For instance field references, the base must match
		if (val instanceof Local)
			if (ap.getPlainValue() != val)
				return null;
		
		// For array references, the base must match
		if (val instanceof ArrayRef)
			if (ap.getPlainValue() != ((ArrayRef) val).getBase())
				return null;
		
		// For instance field references, the base local must match
		if (val instanceof InstanceFieldRef) {
			if (!ap.isLocal() && !ap.isInstanceFieldRef())
				return null;
			if (((InstanceFieldRef) val).getBase() != ap.getPlainValue())
				return null;
		}
		
		// If the value is a static field reference, the access path must be
		// static as well
		if (val instanceof StaticFieldRef)
			if (!ap.isStaticFieldRef())
				return null;
						
		// If we have an interactive aliasing algorithm, we check that as well
		/*
		if (aliasingStrategy.isInteractive())
			return aliasingStrategy.mayAlias(new AccessPath(val1, false), new AccessPath(val2, false));
		*/
		
		// Get the field set from the value
		SootField[] fields = val instanceof FieldRef
				? new SootField[] { ((FieldRef) val).getField() } : new SootField[0];
		return getReferencedAPBase(ap, fields);
	}
	
	/**
	 * Gets whether the two fields must always point to the same runtime object
	 * @param field1 The first field
	 * @param field2 The second field
	 * @return True if the two fields must always point to the same runtime
	 * object, otherwise false
	 */
	public boolean mustAlias(SootField field1, SootField field2) {
		return field1 == field2;
	}

	/**
	 * Gets whether the two values must always point to the same runtime object
	 * @param field1 The first value
	 * @param field2 The second value
	 * @param position The statement at which to check for an aliasing
	 * relationship
	 * @return True if the two values must always point to the same runtime
	 * object, otherwise false
	 */
	public boolean mustAlias(Local val1, Local val2, Stmt position) {
		if (val1 == val2)
			return true;
		if (!(val1.getType() instanceof RefLikeType) || !(val2.getType() instanceof RefLikeType))
			return false;

		LocalMustAliasAnalysis lmaa = strongAliasAnalysis.getUnchecked(cfg.getMethodOf(position));
		return lmaa.mustAlias(val1, position, val2, position);
	}

}
