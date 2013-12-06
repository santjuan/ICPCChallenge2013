public class NearGoldNearEnemyTest extends Game.Strategy
{
	public static void main(String[] args) throws InterruptedException
	{
		RealtimeState state = new RealtimeState();
		NearGoldNearEnemyTest strategy = new NearGoldNearEnemyTest();
		strategy.init(state, state.initial, true);
		state.readTurn(-1);
		while(state.currentTurn >= 0)
			state.readTurn(strategy.getPlay(state));
		System.exit(0);
	}

	@Override
	void init(Game state, Game.InitialState initialState, boolean isRed)
	{
		gold = new NearGoldAdvisor();
		gold.init(initialState, state, isRed);
		defense = new BruteForceDefense(8);
		defense.init(initialState, state, isRed);
	}
}