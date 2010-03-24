package org.qi4j.library.shiro.tests.username;

import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.query.Query;
import org.qi4j.api.query.QueryBuilder;
import org.qi4j.api.query.QueryBuilderFactory;
import static org.qi4j.api.query.QueryExpressions.*;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.library.shiro.domain.RoleAssignee;
import org.qi4j.library.shiro.domain.SecureHashSecurable;
import org.qi4j.library.shiro.realms.AbstractSecureHashQi4jRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRealm
        extends AbstractSecureHashQi4jRealm
{

    private static final Logger LOGGER = LoggerFactory.getLogger( CustomRealm.class );
    @Structure
    private QueryBuilderFactory qbf;

    public CustomRealm()
    {
        super();
        setName( CustomRealm.class.getSimpleName() );
        LOGGER.debug( "CustomRealm instanciated" );
    }

    @Override
    protected SecureHashSecurable getSecureHashSecurable( String username )
    {
        LOGGER.debug( "Loading SecureHashSecurable from username: " + username );
        return findUserEntityByUsername( username );
    }

    @Override
    protected RoleAssignee getRoleAssignee( String username )
    {
        LOGGER.debug( "Loading RoleAssignee from username: " + username );
        return findUserEntityByUsername( username );
    }

    private UserEntity findUserEntityByUsername( String username )
    {
        UnitOfWork uow = uowf.currentUnitOfWork();
        QueryBuilder<UserEntity> queryBuilder = qbf.newQueryBuilder( UserEntity.class );
        UserEntity userTemplate = templateFor( UserEntity.class );
        queryBuilder = queryBuilder.where( eq( userTemplate.username(), username ) );
        Query<UserEntity> query = queryBuilder.newQuery( uow ).maxResults( 1 );
        return query.iterator().next();
    }

}
