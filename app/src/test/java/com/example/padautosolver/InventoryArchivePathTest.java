package com.example.padautosolver;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryArchivePathTest {
    @Test public void canonicalizesBothRootAndEvidence() throws Exception {
        File root = new File("inventory/../inventory");
        File image = new File(root.getCanonicalFile(), "scan_sessions/one/details/held.png");
        assertEquals("scan_sessions/one/details/held.png", InventoryArchivePath.relative(root,image));
    }

    @Test public void rejectsSiblingWithSamePrefix() throws Exception {
        try {
            InventoryArchivePath.relative(new File("inventory"),new File("inventory-backup/held.png"));
            fail("Sibling path must be rejected");
        } catch (IOException expected) { }
    }

    @Test public void rejectsParentTraversal() throws Exception {
        try {
            InventoryArchivePath.relative(new File("inventory"),new File("inventory/../outside.png"));
            fail("Parent traversal must be rejected");
        } catch (IOException expected) { }
    }
}
