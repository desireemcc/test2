from twisted.internet.protocol import Factory, Protocol, ClientCreator
from twisted.internet import reactor, defer
from twisted.python import log
import sys,struct
 
log.startLogging(sys.stdout)
 
class RedirectTx(Protocol):
        tcp_side = None
        def __init__(self, tcp_side):
                self.tcp_side = tcp_side
                self.tcp_side.pipe_side = self
 
        def connectionFailed(self):
                print "Connection Failed:", self
                reactor.stop()
 
        def connectionMade(self):
                self.tcp_side.outConnectionMade()
 
        def dataReceived(self, data):
                if self.tcp_side:
                        self.tcp_side.transport.write(data)
       
        def connectionLost(self, reason):
                print "RedirectTx connection lost: ",reason.value
 
 
class RedirectRx(Protocol):
        def __init__(self):
                self.deferred_receives = []
                self.pipe_side = None
        def connectionMade(self):
                print 'Connection made to RedirectRx'
                d = ClientCreator(reactor, RedirectTx, self).connectTCP(sys.argv[1], int(sys.argv[2]))
                return d
 
        def outConnectionMade(self):
                #print "Out connection established"
                for d in self.deferred_receives:
                        d.callback(None)
 
        def handleDeferredData(self, ignore, data):
                self.dataReceived(data, True)
 
        def dataReceived(self, data, was_deferred=False):
                if self.pipe_side.transport == None:
                        d = defer.Deferred()
                        d.addCallback(self.handleDeferredData, data)
                        self.deferred_receives.append(d)
                        return
                self.pipe_side.transport.write(data)
               
        def connectionLost(self, reason):
                #print 'TCP Connection lost', reason
                pass
 
class RedirectRxFactory(Factory):
        protocol = RedirectRx
 
assert len(sys.argv) == 3, "Usage: redirect_jmx <ip> <port>"
reactor.listenTCP(9999, RedirectRxFactory())
reactor.run()