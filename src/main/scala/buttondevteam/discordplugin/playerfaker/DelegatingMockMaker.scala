package buttondevteam.discordplugin.playerfaker

import org.mockito.MockedConstruction
import org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker
import org.mockito.invocation.MockHandler
import org.mockito.mock.MockCreationSettings
import org.mockito.plugins.MockMaker

import java.util.Optional

object DelegatingMockMaker {
    def getInstance: DelegatingMockMaker = DelegatingMockMaker.instance

    private var instance: DelegatingMockMaker = null
}

class DelegatingMockMaker() extends MockMaker {
    DelegatingMockMaker.instance = this

    override def createMock[T](settings: MockCreationSettings[T], handler: MockHandler[_]): T =
        this.mockMaker.createMock(settings, handler)

    override def createSpy[T](settings: MockCreationSettings[T], handler: MockHandler[_], instance: T): Optional[T] =
        this.mockMaker.createSpy(settings, handler, instance)

    override def getHandler(mock: Any): MockHandler[_] =
        this.mockMaker.getHandler(mock)

    override def resetMock(mock: Any, newHandler: MockHandler[_], settings: MockCreationSettings[_]): Unit = {
        this.mockMaker.resetMock(mock, newHandler, settings)
    }

    override def isTypeMockable(`type`: Class[_]): MockMaker.TypeMockability =
        this.mockMaker.isTypeMockable(`type`)

    override def createStaticMock[T](`type`: Class[T], settings: MockCreationSettings[T], handler: MockHandler[_]): MockMaker.StaticMockControl[T] =
        this.mockMaker.createStaticMock(`type`, settings, handler)

    override def createConstructionMock[T](`type`: Class[T], settingsFactory: java.util.function.Function[MockedConstruction.Context,
        MockCreationSettings[T]], handlerFactory: java.util.function.Function[MockedConstruction.Context,
        MockHandler[T]], mockInitializer: MockedConstruction.MockInitializer[T]): MockMaker.ConstructionMockControl[T] =
        this.mockMaker.createConstructionMock[T](`type`, settingsFactory, handlerFactory, mockInitializer)

    def setMockMaker(mockMaker: MockMaker): Unit = {
        this.mockMaker = mockMaker
    }

    def getMockMaker: MockMaker = this.mockMaker

    private var mockMaker: MockMaker = new SubclassByteBuddyMockMaker
}