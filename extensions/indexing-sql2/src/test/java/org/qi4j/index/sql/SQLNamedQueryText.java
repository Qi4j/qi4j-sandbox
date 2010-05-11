/*
 * Copyright (c) 2010, Stanislav Muhametsin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.qi4j.index.sql;

import org.junit.Ignore;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.spi.query.NamedQueries;
import org.qi4j.spi.query.NamedQueryDescriptor;
import org.qi4j.test.indexing.AbstractNamedQueryTest;

/**
 *
 * @author Stanislav Muhametsin
 */
@Ignore
public class SQLNamedQueryTest extends AbstractNamedQueryTest
{
   private static String[] _queryStrings =
   {
      ""
   };
   
   @Override
   protected NamedQueryDescriptor createNamedQueryDescriptor(String queryName, String queryString)
   {
   // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   protected String[] queryStrings()
   {
      return _queryStrings;
   }
   
   @Override
   protected void setupTest(ModuleAssembly module, NamedQueries namedQueries) throws AssemblyException
   {
      // TODO Auto-generated method stub
      
   }
   
   @Override
   protected void tearDownTest()
   {
      // TODO Auto-generated method stub
      
   }
}
