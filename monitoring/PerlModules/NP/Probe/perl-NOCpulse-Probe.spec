Name:         perl-NOCpulse-Probe
Summary:      Monitoring probes for Spacewalk
URL:          https://fedorahosted.org/spacewalk
Source0:      https://fedorahosted.org/releases/s/p/spacewalk/%{name}-%{version}.tar.gz
Version:      1.184.12
Release:      1%{?dist}
BuildArch:    noarch
Group:        Development/Libraries
%if 0%{?suse_version}
Requires:     perl = %{perl_version}
Requires:     perl-Net-SNMP
Requires:     perl-Net-SSLeay
Requires:     perl-BerkeleyDB
%else
Requires:     perl(:MODULE_COMPAT_%(eval "`%{__perl} -V:version`"; echo $version))
%endif
License:      GPLv2
Buildroot:    %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
Requires(pre): nocpulse-common

%description
NOCpulse provides application, network, systems and transaction monitoring,
coupled with a comprehensive reporting system including availability,
historical and trending reports in an easy-to-use browser interface.

This package provides classes for executing probes.

%package Oracle
Summary:      Monitoring probes for Oracle databases
Group:        Development/Libraries
Requires:     %{name} = %{version}

%description Oracle
NOCpulse provides application, network, systems and transaction monitoring,
coupled with a comprehensive reporting system including availability,
historical and trending reports in an easy-to-use browser interface.

This package provides probes for Oracle.

%prep
%setup -q

%build
#Nothing to build

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Config/test
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/DataSource/test
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/test
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Utils/test
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/SNMP/test
mkdir -p $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/test

install -m 755 -D rhn-runprobe $RPM_BUILD_ROOT%{_bindir}/rhn-runprobe
install -m 755 monitoring-data-cleanup $RPM_BUILD_ROOT%{_bindir}/monitoring-data-cleanup 
install -m 644 Config/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Config/
install -m 644 Config/test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Config/test/
install -m 644 DataSource/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/DataSource/
install -m 644 DataSource/test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/
install -m 644 SNMP/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/SNMP/
install -m 644 SNMP/test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/SNMP/test/
install -m 644 Shell/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/
install -m 644 Shell/test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/test/
install -m 644 Utils/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Utils/
install -m 644 Utils/test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Utils/test/
install -m 644 *.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/
install -m 644 test/*.pm $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/test/

mkdir -p $RPM_BUILD_ROOT%{_mandir}/man3
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/ItemStatus.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::ItemStatus.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/DataSource/MySQL.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::DataSource::MySQL.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/DataSource/NetworkServiceCommand.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::DataSource::NetworkServiceCommand.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Result.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Result.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/AbstractShell.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Shell::AbstractShell.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/SSH.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Shell::SSH.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/SQLPlus.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Shell::SQLPlus.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/Local.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Shell::Local.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT%{perl_vendorlib}/NOCpulse/Probe/Shell/Unix.pm |gzip > $RPM_BUILD_ROOT%{_mandir}/man3/NOCpulse::Probe::Shell::Unix.3pm.gz
/usr/bin/pod2man $RPM_BUILD_ROOT/%{_bindir}/monitoring-data-cleanup | gzip > $RPM_BUILD_ROOT%{_mandir}/man3/monitoring-data-cleanup.3pm.gz

%clean
rm -rf $RPM_BUILD_ROOT

%files 
%defattr(-,root,root,-)
%{_bindir}/rhn-runprobe
%{_bindir}/monitoring-data-cleanup
%dir %{perl_vendorlib}/NOCpulse
%dir %{perl_vendorlib}/NOCpulse/Probe
%dir %{perl_vendorlib}/NOCpulse/Probe/DataSource
%dir %{perl_vendorlib}/NOCpulse/Probe/Shell
%if 0%{?suse_version}
%dir %{perl_vendorlib}/NOCpulse/Probe/DataSource/test
%endif
%{perl_vendorlib}/NOCpulse/Probe/Config*
%{perl_vendorlib}/NOCpulse/Probe/DataSource/AbstractDataSource.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/AbstractDatabase.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/AbstractOSCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/CannedUnixCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/CannedWindowsCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/DfOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/DigOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/EventReaderOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/Factory.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/HTTP.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/InetSocket.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/InterfaceTrafficOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/IostatOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/LogAgentOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/MySQL.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/NetstatOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/NetworkServiceCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/PsOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/SNMP.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/SQLPlusQuery.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/SQLServer.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/SoapLite.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/SwapOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/UnixCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/UptimeOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/VirtualMemoryOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/WQLQuery.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/WindowsCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestInetSocket.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestNetstatOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestPsOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestSNMP.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestSQLPlusQuery.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestSQLServer.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestSwapOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestUnixCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestUnixHelpers.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestVirtualMemoryOutput.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestWindowsCommand.pm
%{perl_vendorlib}/NOCpulse/Probe/*.pm
%{perl_vendorlib}/NOCpulse/Probe/SNMP*
%{perl_vendorlib}/NOCpulse/Probe/Shell/AbstractShell.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/CannedWindowsService.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/Local.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/SSH.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/Unix.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/WindowsService.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/test
%{perl_vendorlib}/NOCpulse/Probe/Shell/test/TestSQLPlus.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/test/TestShell.pm
%{perl_vendorlib}/NOCpulse/Probe/Shell/test/TestWindowsService.pm
%{perl_vendorlib}/NOCpulse/Probe/Utils*
%{perl_vendorlib}/NOCpulse/Probe/test*
%{_mandir}/man3/NOCpulse::Probe::DataSource*
%{_mandir}/man3/NOCpulse::Probe::ItemStatus*
%{_mandir}/man3/NOCpulse::Probe::Result*
%{_mandir}/man3/NOCpulse::Probe::Shell::AbstractShell*
%{_mandir}/man3/NOCpulse::Probe::Shell::Local*
%{_mandir}/man3/NOCpulse::Probe::Shell::SSH*
%{_mandir}/man3/NOCpulse::Probe::Shell::Unix*
%{_mandir}/man3/monitoring-data-cleanup*
%{_mandir}/man3/NOCpulse::Probe::Shell::SQLPlus*
%doc LICENSE

%files Oracle
%defattr(-,root,root,-)
%{perl_vendorlib}/NOCpulse/Probe/DataSource/CannedOracle.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/Oracle.pm
%{perl_vendorlib}/NOCpulse/Probe/DataSource/test/TestOracle.pm

%changelog
* Wed Sep 14 2011 Jan Pazdziora 1.184.12-1
- The NOCpulse::Probe::Shell::SQLPlus needs to be in perl-NOCpulse-Probe, not
  in -Oracle.
- Revert "remove duplicated SQLPlus.pm file from main package as it is already
  in -Oracle" and "Addressing warning: File listed twice caused by the previous
  commit."

* Tue Sep 13 2011 Jan Pazdziora 1.184.11-1
- Addressing warning: File listed twice caused by the previous commit.
- remove duplicated SQLPlus.pm file from main package as it is already in
  -Oracle (iartarisi@suse.cz)

* Thu Aug 11 2011 Jan Pazdziora 1.184.10-1
- The column names are always uppercase, due to the FetchHashKeyName setting.

* Tue Jul 19 2011 Jan Pazdziora 1.184.9-1
- Updating the copyright years.

* Mon May 16 2011 Jan Pazdziora 1.184.8-1
- The database handle now has AutoCommit turned off, so begin_work is not
  needed anymore.

* Fri Mar 18 2011 Michael Mraka <michael.mraka@redhat.com> 1.184.7-1
- fixed db connection in monitoring-data-cleanup (PG)

* Fri Feb 18 2011 Jan Pazdziora 1.184.6-1
- Localize the filehandle globs; also use three-parameter opens.
- Changing array with map is ugly, we shall use for instead.

* Tue Nov 02 2010 Jan Pazdziora 1.184.5-1
- 612581 - change egrep to grep -E (msuchy@redhat.com)

* Mon Sep 27 2010 Miroslav Suchý <msuchy@redhat.com> 1.184.4-1
- 636224 - fix typos in mange and output of --help

* Fri Aug 06 2010 Miroslav Suchý <msuchy@redhat.com> 1.184.3-1
- 530519 - strip spaces from ipaddr

* Mon Jul 12 2010 Miroslav Suchý <msuchy@redhat.com> 1.184.2-1
- move test/TestOracle.pm to -Oracle subpackage (msuchy@redhat.com)

* Mon Jul 12 2010 Miroslav Suchý <msuchy@redhat.com> 1.184.1-1
- code cleanup - attribute provided is not used anywhere (msuchy@redhat.com)
- split from perl-NOCpulse-Probe new subpackage perl-NOCpulse-Probe-Oracle,
  which contains Oracle Probes (msuchy@redhat.com)

* Thu Feb 04 2010 Michael Mraka <michael.mraka@redhat.com> 1.183.13-1
- updated copyrights

* Thu Jan  7 2010 Miroslav Suchý <msuchy@redhat.com> 1.183.12-1
- enhance monitoring-data-cleanup to allow delete in smaller chunk

* Fri Aug 28 2009 Miroslav Suchý <msuchy@redhat.com> 1.183.11-1
- 520101 - add monitoring-data-cleanup script

* Thu Jul 23 2009 Miroslav Suchý <msuchy@redhat.com> 1.183.9-1
- 512749 -  fix path to file during man page generation

* Wed Apr 15 2009 Devan Goodwin <dgoodwin@redhat.com> 1.183.7-1
- fix various perl -w warnings (msuchy@redhat.com)
- Fix ownership of nocpulse directory (msuchy@redhat.com)
- Add LICENSE file (msuchy@redhat.com)
- Remove unused macro definition (msuchy@redhat.com)

* Tue Jan 13 2009 Milan Zazrivec 1.183.6-1
- point ssh to correct private key

* Tue Jan 13 2009 Miroslav Suchý <msuchy@redhat.com> 1.183.5-1
- 479441 - this one should not be part of namespace fix

* Tue Dec 16 2008 Miroslav Suchý <msuchy@redhat.com> 1.183.4-1
- 472895 - remove grouped_fields from Class::MethodMaker declaration

* Mon Oct 20 2008 Miroslav Suchý <msuchy@redhat.com> 1.183.3-1
- 467441 - fix namespace

* Tue Sep  2 2008 Miroslav Suchý <msuchy@redhat.com> 1.183.2-1
- edit spec to comply with Fedora guidelines

* Thu Jun 19 2008 Miroslav Suchy <msuchy@redhat.com>
- migrating nocpulse home dir (BZ 202614)

* Wed Jun  4 2008 Milan Zazrivec <mzazrivec@redhat.com> 1.183.1-22
- fixed file permissions

* Wed May 28 2008 Jan Pazdziora 1.183.1-21
- rebuild in dist-cvs
