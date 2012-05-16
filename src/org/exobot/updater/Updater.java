package org.exobot.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import org.exobot.game.loader.ClientLoader;
import org.exobot.updater.container.HookContainer;
import org.exobot.updater.processor.AddGetterProcessor;
import org.exobot.updater.processor.Processor;
import org.exobot.updater.processor.SetSignatureProcessor;
import org.exobot.util.ExoMap;
import org.exobot.util.RIS;
import org.exobot.util.io.Package;
import org.exobot.util.io.StreamWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class Updater extends Thread implements Runnable {

	private static Updater instance;

	public static Updater getInstance() {
		return instance;
	}

	public static void main(final String[] args) {
		instance = new Updater();
		instance.start();
	}

	private final List<HookContainer> containers = new LinkedList<HookContainer>();
	private final ExoMap<String, ClassNode> classes = new ExoMap<String, ClassNode>();

	@Override
	public void run() {
		System.out.println("[ - Ramus' ASM Updater - ]");
		System.out.println();
		final ClientLoader loader = new ClientLoader();
		classes.putAll(loader.getClasses());
		loadContainers();
		executeContainers();
		generateModscript();
	}

	private void executeContainers() {
		System.out.println("[ - RuneScape #" + getRSBuild() + " - ]");
		System.out.println();
		System.out.println("Executing containers.");
		System.out.println();
		HookContainer.sort(containers);
		Map<String, ClassNode> classes = new ExoMap<String, ClassNode>(this.classes);
		final long startTime = System.currentTimeMillis();
		for (final HookContainer hc : containers) {
			for (final Map.Entry<String, ClassNode> entry : classes.entrySet()) {
				final String name = entry.getKey();
				final ClassNode cn = entry.getValue();
				if (!hc.validate(name, cn)) {
					continue;
				}
				for (final Task task : hc.getTasks()) {
					task.run(name, cn);
				}
				classes = new ExoMap<String, ClassNode>(this.classes);
			}
		}
		this.classes.clear();
		this.classes.putAll(classes);
		final double finishTime = (System.currentTimeMillis() - startTime) / 1000.0;
		int totalGetters = 0;
		int totalClasses = 0;
		int totalSigs = 0;
		for (final HookContainer hc : containers) {
			if (hc.isHidden()) {
				continue;
			}
			totalGetters += hc.getGetters();
			totalClasses += hc.getInterfaces() + hc.getSupers();
			totalSigs += hc.getSignatures();
		}
		int getters = 0;
		int clazzes = 0;
		int sigs = 0;
		Collections.sort(containers, new Comparator<HookContainer>() {

			@Override
			public int compare(final HookContainer hc1, final HookContainer hc2) {
				return hc1.getName().compareTo(hc2.getName());
			}
		});
		for (final HookContainer hc : containers) {
			if (hc.isHidden()) {
				continue;
			}
			System.out.println("[ - " + hc.getName() + " - ]");
			for (final Processor p : hc.getProcessors()) {
				if (p.getOutput() == null || p.getOutput().isEmpty()) {
					continue;
				}
				switch (p.getId()) {
					case Processor.Id.GET_STATIC:
					case Processor.Id.GET_FIELD:
						getters++;
						break;
					case Processor.Id.SET_SUPER:
					case Processor.Id.ADD_INTERFACE:
						clazzes++;
						break;
					case Processor.Id.SET_SIGNATURE:
						sigs++;
						break;
				}
				System.out.println(p.getOutput());
			}
			System.out.println();
		}
		System.out.println("Successfully executed containers in " + finishTime + " seconds.");
		System.out.println();
		System.out.println("Successfully identified " + clazzes + "/" + totalClasses + " classes.");
		System.out.println("Successfully identified " + getters + "/" + totalGetters + " fields.");
		System.out.println("Successfully identified " + sigs + "/" + totalSigs + " methods.");
		System.out.println();
	}

	private void generateModscript() {
		try {
			System.out.println("Generating modscript.");
			final StreamWriter stream = new StreamWriter();
			final List<Processor> processors = new LinkedList<Processor>();
			int numProcessors = 0;
			for (final HookContainer hc : containers) {
				if (hc.isHidden()) {
					continue;
				}
				boolean getters = true;
				for (final Processor p : hc.getProcessors()) {
					if (p instanceof AddGetterProcessor) {
						if (getters) {
							numProcessors++;
							getters = false;
						}
						continue;
					}
					numProcessors++;
				}
				processors.addAll(hc.getProcessors());
			}
			Collections.sort(processors, new Comparator<Processor>() {

				@Override
				public int compare(final Processor p, final Processor q) {
					return Integer.compare(p.getId(), q.getId());
				}
			});
			stream.writeInt(0xFADFAD);
			stream.writeString("ramus-exobot-v1.ms");
			stream.writeShort(getRSBuild());
			stream.writeShort(numProcessors);
			stream.write(0xA);
			HookContainer getterContainer = null;
			HookContainer sigContainer = null;
			for (final Processor p : processors) {
				if (p instanceof AddGetterProcessor) {
					if (!p.getContainer().equals(getterContainer)) {
						getterContainer = p.getContainer();
						stream.write(p.getId());
						stream.writeString(classes.containsKey(getterContainer.getName()) ? classes.get(getterContainer.getName()).name : getterContainer.getName().toLowerCase());
						stream.writeShort(getterContainer.getSize(AddGetterProcessor.class));
					}
				} else if (p instanceof SetSignatureProcessor) {
					if (!p.getContainer().equals(sigContainer)) {
						sigContainer = p.getContainer();
						stream.write(p.getId());
						stream.writeString(classes.containsKey(sigContainer.getName()) ? classes.get(sigContainer.getName()).name : sigContainer.getName().toLowerCase());
						stream.writeShort(sigContainer.getSize(SetSignatureProcessor.class));
					}
				}
				p.process(stream);
			}
			stream.subSequence(0, stream.size() - 1);
			stream.writeTo(new FileOutputStream(new File("modscript.dat")));
			System.out.print("Successfully generated modscript.");
		} catch (final Exception e) {
			System.out.print("Unable generate modscript.");
			e.printStackTrace();
		}
	}

	public ExoMap<String, ClassNode> getClasses() {
		return classes;
	}

	private int getRSBuild() {
		final ClassNode client = classes.get("client");
		for (final MethodNode mn : client.methods) {
			if (!mn.name.equals("init") || !mn.desc.equals("()V")) {
				continue;
			}
			final RIS ris = new RIS(mn);
			IntInsnNode sipush;
			ris.setPosition(ris.getInsnList().size() - 1);
			while ((sipush = ris.previous(IntInsnNode.class, Opcodes.SIPUSH)) != null) {
				final int value = sipush.operand;
				if (value > 250 && value < 2500) {
					return value;
				}
			}
		}
		return -1;
	}

	private void loadContainers() {
		System.out.println("Loading containers.");
		try {
			final List<Class<?>> classes = Package.getClasses("org.exobot.updater.container");
			for (final Class<?> clazz : classes) {
				try {
					final Object inst = clazz.newInstance();
					if (inst instanceof HookContainer) {
						containers.add((HookContainer) inst);
					}
				} catch (final Exception ignored) {
				}
			}
			System.out.println("Successfully loaded " + containers.size() + " containers.");
		} catch (final IOException e) {
			System.out.println("Unable to load containers.");
		}
		System.out.println();
	}
}