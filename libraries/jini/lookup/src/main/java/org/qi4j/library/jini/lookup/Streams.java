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

package org.qi4j.library.jini.lookup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams
{
    public static void copyStream( InputStream src, OutputStream dest, boolean closeAfterCopy )
        throws IOException
    {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try
        {
            if( src instanceof BufferedInputStream )
            {
                in = (BufferedInputStream) src;
            }
            else
            {
                in = new BufferedInputStream( src );
            }
            if( dest instanceof BufferedOutputStream )
            {
                out = (BufferedOutputStream) dest;
            }
            else
            {
                out = new BufferedOutputStream( dest );
            }

            byte[] buffer = new byte[1024];
            int count = 0;
            do
            {
                count = in.read( buffer );
                if( count > 0 )
                {
                    out.write( buffer, 0, count );
                }
            }
            while( count != -1 );
            out.flush();
        }
        finally
        {
            if( closeAfterCopy )
            {
                if( in != null )
                {
                    in.close();
                }
                if( out != null )
                {
                    out.close();
                }
            }
        }
    }
}
