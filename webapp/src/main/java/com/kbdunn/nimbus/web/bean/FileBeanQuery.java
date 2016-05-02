package com.kbdunn.nimbus.web.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;

public class FileBeanQuery extends AbstractBeanQuery<FileBean> {

	private static final long serialVersionUID = -2892396790828716083L;
	public static final String ROOT_CONTAINER_KEY = "root";
	
	public FileBeanQuery(QueryDefinition definition, Map<String, Object> queryConfiguration, 
			Object[] sortPropertyIds, boolean[] sortStates) {
		super(definition, queryConfiguration, sortPropertyIds, sortStates);
	}
	
	@Override
	protected FileBean constructBean() {
		return new FileBean();
	}
	
	@Override
	protected List<FileBean> loadBeans(int startIndex, int count) {
		/*log.debug("loadBeans(" + startIndex + ", " + count + ") called.");
		for (int i = 0; i < getSortPropertyIds().length; i++) {
			log.debug("Sort property " + getSortPropertyIds()[i] + "=" + getSortStates()[i]);
		}*/
		List<FileBean> beans = new ArrayList<FileBean>();
		List<NimbusFile> files = null;
		FileContainer root = getRootContainer();
		if (root instanceof NimbusFile) 
			files = NimbusUI.getFileService().getContents((NimbusFile) root, startIndex, count);
		// ShareBlock not implemented
		//else if (root instanceof ShareBlock) files = NimbusUI.getCurrentFileShareService().getContents((ShareBlock) root, startIndex, count);
		else throw new IllegalStateException("Value of key " + ROOT_CONTAINER_KEY + " is not an instance of NimbusFile");
		
		for (NimbusFile nf : files) {
			beans.add(new FileBean(nf));
		}
		return beans;
	}
	
	@Override
	protected void saveBeans(List<FileBean> added, List<FileBean> modified, List<FileBean> removed) {
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	@Override
	public int size() {
		FileContainer root = getRootContainer();
		if (root instanceof NimbusFile) return NimbusUI.getFileService().getContentCount((NimbusFile) root);
		else if (root instanceof ShareBlock) return NimbusUI.getFileShareService().getContentCount((ShareBlock) root);
		throw new IllegalStateException("Value of key " + ROOT_CONTAINER_KEY + " is not an instance of FileContainer");
	}
	
	private FileContainer getRootContainer() {
		FileContainer root = (FileContainer) getQueryConfiguration().get(ROOT_CONTAINER_KEY);
		if (root == null) throw new IllegalStateException("The query configuration does not contain the " + ROOT_CONTAINER_KEY + " key");
		return root;
	}
}
