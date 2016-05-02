package com.kbdunn.nimbus.web.files.action;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;

public class ZipDownloadButton extends Button {

	private static final long serialVersionUID = -4381830928361612946L;
	
	public ZipDownloadButton(String caption, NimbusFile file, String fileName) {
		super(caption);

		List<NimbusFile> contents = new ArrayList<NimbusFile>();
		contents.add(file);
		
		if (!NimbusUI.getPropertiesService().isDemoMode()) {
			InputStream is = NimbusUI.getFileService().getZipComressedInputStream(contents);
			StreamSource stream = new ZipStreamSource(is);
			FileDownloader fd = new FileDownloader(new StreamResource(stream, fileName));
			fd.extend(this);
		}
		
		setIcon(getIcon());
	}
	
	public ZipDownloadButton(String caption, List<NimbusFile> contents, String fileName) {
		super(caption);

		if (!NimbusUI.getPropertiesService().isDemoMode()) {
			InputStream is = NimbusUI.getFileService().getZipComressedInputStream(contents);
			StreamSource stream = new ZipStreamSource(is);
			FileDownloader fd = new FileDownloader(new StreamResource(stream, fileName));
			fd.extend(this);
		}
		
		setIcon(getIcon());
	}
	
	public String getCaption() {
		return "Download Files as ZIP";
	}
	
	public FontAwesome getIcon() {
		return FontAwesome.FILE_ARCHIVE_O;
	}
	
	class ZipStreamSource implements StreamSource {
		private static final long serialVersionUID = -3272580672714403917L;
		InputStream is;
		
		public ZipStreamSource(InputStream is) {
			this.is = is;
		}
		
		@Override
		public InputStream getStream() {
			return is;
		}
	}
}