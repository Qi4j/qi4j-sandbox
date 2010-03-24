package org.qi4j.library.shiro.tests.username;

import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.property.Property;
import org.qi4j.library.shiro.domain.RoleAssignee;
import org.qi4j.library.shiro.domain.SecureHashSecurable;

public interface UserEntity
        extends RoleAssignee, SecureHashSecurable, EntityComposite
{

    Property<String> username();

}
