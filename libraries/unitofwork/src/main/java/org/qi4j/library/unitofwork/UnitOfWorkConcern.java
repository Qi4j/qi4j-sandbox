/*
 * Copyright 2008 Edward Yakop.
 * Copyright 2009 Niclas Hedhman.
 * Copyright 2009 Michael Hunger.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.library.unitofwork;

import java.lang.reflect.Method;
import org.qi4j.api.common.AppliesTo;
import org.qi4j.api.concern.GenericConcern;
import org.qi4j.api.injection.scope.Invocation;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;

/**
 * {@code UnitOfWorkConcern} manages the unit of work complete and discard policy.
 *
 * @see org.qi4j.api.unitofwork.UnitOfWorkPropagation
 * @see org.qi4j.api.unitofwork.UnitOfWorkDiscardOn
 */
@AppliesTo( UnitOfWorkPropagation.class )
public class UnitOfWorkConcern
    extends GenericConcern
{
    @Structure
    private UnitOfWorkFactory uowf;
    @Invocation
    private UnitOfWorkPropagation propagation;

    /**
     * Handles method with {@code UnitOfWorkPropagation} annotation.
     *
     * @param proxy  The object.
     * @param method The invoked method.
     * @param args   The method arguments.
     *
     * @return The returned value of method invocation.
     *
     * @throws Throwable Thrown if the method invocation throw exception.
     */
    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        UnitOfWorkPropagation.Propagation propagationPolicy = propagation.value();
        if( propagationPolicy == UnitOfWorkPropagation.Propagation.REQUIRED )
        {
            return requiredStrategy( proxy, method, args );
        }
        else if( propagationPolicy == UnitOfWorkPropagation.Propagation.MANDATORY )
        {
            return mandatoryStrategy( proxy, method, args );
        }
        else if( propagationPolicy == UnitOfWorkPropagation.Propagation.REQUIRES_NEW )
        {
            return requiresNewRequires( proxy, method, args );
        }
        throw new UnitOfWorkPropagationException( "'null' is not allowed as propagation strategy." );
    }

    /**
     * Discard unit of work if the discard policy match.
     *
     * @param aMethod         The invoked method. This argument must not be {@code null}.
     * @param aUnitOfWork     The current unit of work. This argument must not be {@code null}.
     * @param exceptionThrown The exception thrown. This argument must not be {@code null}.
     *
     * @throws org.qi4j.api.unitofwork.UnitOfWorkCompletionException
     *          If the complete() method fails.
     */
    private void discardIfRequired( Method aMethod, UnitOfWork aUnitOfWork, Throwable exceptionThrown )
        throws UnitOfWorkCompletionException
    {
        UnitOfWorkDiscardOn discardPolicy = aMethod.getAnnotation( UnitOfWorkDiscardOn.class );
        UnitOfWorkNoDiscardOn noDiscardPolicy = aMethod.getAnnotation( UnitOfWorkNoDiscardOn.class );
        if( null == discardPolicy && noDiscardPolicy == null )
        {
            aUnitOfWork.discard();
            return;
        }
        Class<?>[] discardClasses;
        if( discardPolicy == null )
        {
            discardClasses = new Class[]{ Throwable.class };
        }
        else
        {
            discardClasses = discardPolicy.value();
        }
        Class<?>[] noDiscardClasses;
        if( noDiscardPolicy == null )
        {
            noDiscardClasses = new Class[0];
        }
        else
        {
            noDiscardClasses = noDiscardPolicy.value();
        }

        Class<? extends Throwable> thrownClass = exceptionThrown.getClass();

        next:
        for( Class<?> discardClass : discardClasses )
        {
            if( discardClass.isAssignableFrom( thrownClass ) )
            {
                for( Class<?> noDiscardClass : noDiscardClasses )
                {
                    if( noDiscardClass.isAssignableFrom( thrownClass ) )
                    {
                        continue next;
                    }
                }
                aUnitOfWork.discard();
                return;
            }
        }
        aUnitOfWork.complete();
    }

    private UnitOfWork createNewUnitOfWork()
    {
        return uowf.newUnitOfWork();
    }

    private Object requiresNewRequires( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        UnitOfWork currentUnitOfWork = createNewUnitOfWork();
        try
        {
            Object result = next.invoke( proxy, method, args );
            currentUnitOfWork.complete();
            return result;
        }
        catch( Throwable throwable )
        {
            discardIfRequired( method, currentUnitOfWork, throwable );
            throw throwable;
        }
    }

    private Object mandatoryStrategy( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        UnitOfWork currentUnitOfWork = uowf.currentUnitOfWork();
        if( currentUnitOfWork == null )
        {
            throw new UnitOfWorkPropagationException( "[UnitOfWork] was required but there is no available unit of work." );
        }
        return next.invoke( proxy, method, args );
    }

    private Object requiredStrategy( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        UnitOfWork currentUnitOfWork = uowf.currentUnitOfWork();
        boolean created = false;
        if( currentUnitOfWork == null )
        {
            currentUnitOfWork = createNewUnitOfWork();
            created = true;
        }

        try
        {
            Object result = next.invoke( proxy, method, args );
            if( created )
            {
                currentUnitOfWork.complete();
            }
            return result;
        }
        catch( Throwable throwable )
        {
            // Discard only if this concern create a unit of work
            if( created )
            {
                discardIfRequired( method, currentUnitOfWork, throwable );
            }
            throw throwable;
        }
    }
}
