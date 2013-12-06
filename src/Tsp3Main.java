public class Tsp3Main extends Game.Strategy
{
	public static void main(String[] args)
	{
		RealtimeState state = new RealtimeState();
		Tsp3Main strategy = new Tsp3Main();
		strategy.init(state, state.initial, true);
		state.readTurn(-1);
		System.err.println(state.currentTurn + " " + state.initial.turnCount);
		while(state.currentTurn >= 0)
		{
			int adv = strategy.getPlay(state);
			state.readTurn(adv);
		}
		System.exit(0);
	}

	@Override
	void init(Game state, Game.InitialState initialState, boolean isRed) 
	{
		gold = new TspNp3Advisor(4);
		gold.init(initialState, state, isRed);
		defense = new BruteForceDefense(8);
		defense.init(initialState, state, isRed);
	}
}
