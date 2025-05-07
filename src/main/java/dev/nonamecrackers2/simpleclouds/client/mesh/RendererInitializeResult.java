package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;

public class RendererInitializeResult
{
	private final RendererInitializeResult.State state;
	private final List<RendererInitializeResult.Error> errors;
	private @Nullable List<Path> savedReportsPaths;
	private @Nullable List<CrashReport> crashReports;
	
	private RendererInitializeResult(RendererInitializeResult.State state, List<RendererInitializeResult.Error> errors)
	{
		this.state = state;
		this.errors = errors;
	}
	
	public RendererInitializeResult.State getState()
	{
		return this.state;
	}
	
	public List<RendererInitializeResult.Error> getErrors()
	{
		return this.errors;
	}
	
	public List<CrashReport> createCrashReports()
	{
		List<CrashReport> reports = Lists.newArrayList();
		for (RendererInitializeResult.Error error : this.errors)
		{
			CrashReport report = CrashReport.forThrowable(error.error(), "Simple Clouds mesh generator initialization; " + error.title());
			CrashReportCategory category = report.addCategory("Initialization details");
			category.setDetail("Recommendation", error.text().getString());
			if (this.errors.size() > 1)
				category.setDetail("Notice", "Multiple crash reports have been generated during mesh generator initialization");
			reports.add(report);
		}
		this.crashReports = reports;
		return reports;
	}
	
	public void saveCrashReports(File gameDirectory)
	{
		this.savedReportsPaths = Lists.newArrayList();
		boolean flag = this.crashReports.size() > 1;
		for (int i = 0; i < this.crashReports.size(); i++)
		{
			CrashReport report = this.crashReports.get(i);
			File crashReportPath = new File(gameDirectory, "crash-reports");
			String fileName = "crash-" + Util.getFilenameFormattedDateTime() + "-simpleclouds-mesh-generator";
			if (flag)
				fileName += "-" + i + ".txt";
			else
				fileName += ".txt";
			File file = new File(crashReportPath, fileName);
			if (report.getSaveFile() == null)
			{
				report.saveToFile(file);
				this.savedReportsPaths.add(file.toPath());
			}
		}
	}
	
	public @Nullable List<Path> getSavedCrashReportPaths()
	{
		return this.savedReportsPaths;
	}
	
	public static RendererInitializeResult success()
	{
		return new RendererInitializeResult(RendererInitializeResult.State.SUCCESS, ImmutableList.of());
	}
	
	public static RendererInitializeResult.Builder builder()
	{
		return new RendererInitializeResult.Builder();
	}
	
	public static record Error(@Nullable Throwable error, String title, @Nullable Component text) {}
	
	public static enum State
	{
		SUCCESS,
		ERROR;
	}
	
	public static class Builder
	{
		private RendererInitializeResult.State state = RendererInitializeResult.State.SUCCESS;
		private final ImmutableList.Builder<RendererInitializeResult.Error> errors = ImmutableList.builder();
		
		private Builder() {}
		
		public Builder addError(@Nullable Throwable error, String title, Component text)
		{
			this.errors.add(new RendererInitializeResult.Error(error, title, Objects.requireNonNull(text)));
			this.state = RendererInitializeResult.State.ERROR;
			return this;
		}
		
		public Builder errorUnknown(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.unknown"));
		}
		
		public Builder errorRecommendations(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.recommendations"));
		}
		
		public Builder errorOpenGL()
		{
			return this.addError(null, "Outdated OpenGL Version", Component.translatable("gui.simpleclouds.error.opengl"));
		}
		
		public Builder errorCouldNotLoadMeshScript(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.couldNotLoadMeshScript"));
		}
		
		public RendererInitializeResult build()
		{
			return new RendererInitializeResult(this.state, this.errors.build());
		}
	}
}
