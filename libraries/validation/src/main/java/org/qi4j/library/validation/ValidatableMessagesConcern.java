package org.qi4j.library.validation;

import org.qi4j.api.common.AppliesTo;
import org.qi4j.api.concern.ConcernOf;
import org.qi4j.api.injection.scope.This;

import java.util.List;

/**
 * JAVADOC
 */
@AppliesTo( Validatable.class )
public abstract class ValidatableMessagesConcern extends ConcernOf<Validatable>
    implements Validatable
{
    @This ValidationMessages messages;

    public List<ValidationMessage> validate()
    {
        List<ValidationMessage> messageList = next.validate();
        messageList.addAll( messages.getValidationMessages() );
        return messageList;
    }
}
