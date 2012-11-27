package de.zilant.icanword;


public class Definition extends net.jeremybrooks.knicker.dto.Definition
{
	public Definition(boolean needToMemorize)
	{
		super();
		this.needToMemorize = needToMemorize;
		this.isAltered = false;
		this.id = -1;
	}
	
	public Definition(net.jeremybrooks.knicker.dto.Definition definition)
	{
		this(false);
		setAttributionText(definition.getAttributionText());
		setPartOfSpeech(definition.getPartOfSpeech());
		setScore(definition.getScore());
		setSequence(definition.getSequence());
		setSourceDictionary(definition.getSourceDictionary());
		setText(definition.getText());
		setWord(definition.getWord());
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return id; }
	
	public boolean setNeedToMemorize(boolean doesNeed)
	{
		if(needToMemorize == doesNeed)
			return false;
		needToMemorize = doesNeed;
		isAltered = !isAltered;
		return true;
	}
	
	public boolean getNeedToMemorize() { return needToMemorize; }
	
	public boolean isAltered() { return isAltered; }
	
	private boolean needToMemorize;
	private boolean isAltered;
	private long id;
	
	private static final long serialVersionUID = 1L;
}
