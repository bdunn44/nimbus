package com.kbdunn.nimbus.web.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.kbdunn.nimbus.common.model.Song;

public class SongBeanQuery extends AbstractBeanQuery<SongBean> {

	private static final long serialVersionUID = 4067573247415399377L;
	public static final String CONTROLLER_KEY = "controller";
	
	public SongBeanQuery(QueryDefinition definition, Map<String, Object> queryConfiguration, 
			Object[] sortPropertyIds, boolean[] sortStates) {
		super(definition, queryConfiguration, sortPropertyIds, sortStates);
	}
	
	@Override
	protected SongBean constructBean() {
		return new SongBean();
	}
	
	@Override
	protected List<SongBean> loadBeans(int startIndex, int count) {
		List<SongBean> beans = new ArrayList<SongBean>();
		for (Song s : getController().getCurrentSongs(this, startIndex, count)) {
			beans.add(new SongBean(s));
		}
		return beans;
	}
	
	@Override
	protected void saveBeans(List<SongBean> added, List<SongBean> modified, List<SongBean> removed) {
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	@Override
	public int size() {
		return getController().getCurrentSongCount(this);
	}
	
	private SongBeanQueryController getController() {
		SongBeanQueryController c = (SongBeanQueryController) getQueryConfiguration().get(CONTROLLER_KEY);
		if (c == null) throw new IllegalStateException("The query configuration does not contain the " + CONTROLLER_KEY + " key");
		return c;
	}
	
	public interface SongBeanQueryController {
		public List<Song> getCurrentSongs(SongBeanQuery instance, int startIndex, int count);
		public int getCurrentSongCount(SongBeanQuery instance);
	}
}
