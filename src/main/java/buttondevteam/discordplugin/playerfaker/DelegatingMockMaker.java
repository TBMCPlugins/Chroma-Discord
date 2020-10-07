package buttondevteam.discordplugin.playerfaker;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker;
import org.mockito.plugins.MockMaker;

public class DelegatingMockMaker implements MockMaker {
	@Getter
	@Setter
	@Delegate
	private MockMaker mockMaker = new SubclassByteBuddyMockMaker();
	@Getter
	private static DelegatingMockMaker instance;

	public DelegatingMockMaker() {
		instance = this;
	}
}
