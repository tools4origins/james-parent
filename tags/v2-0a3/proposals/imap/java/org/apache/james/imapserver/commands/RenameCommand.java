/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AuthorizationException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.MailboxException;

import java.util.StringTokenizer;
import java.util.List;

class RenameCommand extends AuthenticatedSelectedStateCommand
{
    public RenameCommand()
    {
        this.commandName = "RENAME";

        this.getArgs().add( new AstringArgument( "oldName" ) );
        this.getArgs().add( new AstringArgument( "newName" ) );
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        String folder = (String) argValues.get( 0 );
        String newName = (String) argValues.get( 1 );

        if ( session.getCurrentFolder() == folder ) {
            session.noResponse( command, "You can't rename a folder while you have it selected." );
            return true;
        }
        try {
            if ( session.getImapHost().renameMailbox( session.getCurrentUser(), folder, newName ) ) {
                session.okResponse( command );
            }
            else {
                session.noResponse( command, "Rename failed, unknown error" );
                getLogger().info( "Attempt to rename mailbox " + folder
                                  + " to " + newName
                                  + " by user " + session.getCurrentUser() + " failed." );
            }
        }
        catch ( MailboxException mbe ) {
            if ( mbe.getStatus().equals( MailboxException.NOT_LOCAL ) ) {
                session.taggedResponse( NO_NOTLOCAL_MSG );
            }
            else {
                session.noResponse( command, mbe.getMessage() );
            }
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to delete mailbox: " + folder );
            return true;
        }
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
