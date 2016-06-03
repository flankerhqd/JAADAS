/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

/**	Semantic annotation that the annotated field is synchronized.
 *  This annotation is meant as a structured comment only, and has no immediate effect. */
@Target(FIELD)
public @interface SynchronizedBy{ String value() default ""; }
