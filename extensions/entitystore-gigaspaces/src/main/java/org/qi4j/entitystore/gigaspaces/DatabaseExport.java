/*
 * Copyright 2009 Niclas Hedhman.
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

package org.qi4j.entitystore.gigaspaces;

import java.io.IOException;
import java.io.Writer;

public interface DatabaseExport
{
    /**
     * Export data to the writer, with one line per object, in JSON format.
     *
     * @param out the destination to write the data to.
     * @throws java.io.IOException if problems in the underlying stream.
     */
    void exportTo( Writer out )
        throws IOException;
}
