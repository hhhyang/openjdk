/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @bug 4120329
 * @summary RMI registry creation is impossible if first attempt fails.
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary RegistryVM RegistryRunner
 * @run main/othervm Reexport
 */

/*
 * If a VM could not create an RMI registry because another registry
 * usually in another process, was using the registry port, the next
 * time the VM tried to create a registry (after the other registry
 * was brought down) the attempt would fail.  The second try to create
 * a registry would fail because the registry ObjID would still be in
 * use when it should never have been allocated.
 *
 * The test creates this conflict using Runtime.exec and ensures that
 * a registry can still be created after the conflict is resolved.
 */

import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Reexport {
    static public void main(String[] argv) {

        Registry reg = null;
        try {
            System.err.println("\nregression test for 4120329\n");

            // establish the registry (we hope)
            makeRegistry();

            // Get a handle to the registry
            System.err.println("Creating duplicate registry, this should fail...");
            reg = createReg(true);

            // Kill the first registry.
            System.err.println("Bringing down the first registry");
            try {
                killRegistry();
            } catch (Exception foo) {
            }

            // start another registry now that the first is gone; this should work
            System.err.println("Trying again to start our own " +
                               "registry... this should work");

            reg = createReg(false);

            if (reg == null) {
                TestLibrary.bomb("Could not create registry on second try");
            }

            System.err.println("Test passed");

        } catch (Exception e) {
            TestLibrary.bomb(e);
        } finally {
            // dont leave the registry around to affect other tests.
            killRegistry();
            reg = null;
        }
    }

    static Registry createReg(boolean remoteOk) {
        Registry reg = null;

        try {
            reg = LocateRegistry.createRegistry(port);
            if (remoteOk) {
                TestLibrary.bomb("Remote registry is up, an Exception is expected!");
            }
        } catch (Throwable e) {
            if (remoteOk) {
                System.err.println("EXPECTING PORT IN USE EXCEPTION:");
                System.err.println(e.getMessage());
                e.printStackTrace();
            } else {
                TestLibrary.bomb((Exception) e);
            }
        }
        return reg;
    }

    public static void makeRegistry() {
        try {
            subreg = RegistryVM.createRegistryVM();
            subreg.start();
            port = subreg.getPort();
            System.out.println("Starting registry on port " + port);
        } catch (IOException e) {
            // one of these is summarily dropped, can't remember which one
            System.out.println ("Test setup failed - cannot run rmiregistry");
            TestLibrary.bomb("Test setup failed - cannot run test", e);
        }
    }

    private static RegistryVM subreg = null;
    private static int port = -1;

    public static void killRegistry() {
        if (subreg != null) {
            subreg.cleanup();
            subreg = null;
        }
    }
}
