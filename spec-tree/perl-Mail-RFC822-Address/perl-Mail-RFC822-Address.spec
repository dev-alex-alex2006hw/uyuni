%{!?perlgen:%global perlgen 5.8}
Name: perl-Mail-RFC822-Address
Version: 0.3
Release: 14%{?dist}
Summary: Mail-RFC822-Address Perl module
License: distributable
Group: Development/Libraries
URL: http://search.cpan.org/search?mode=module&query=Mail%3a%3aRFC822%3a%3aAddress
BuildRoot: %{_tmppath}/%{name}-root
Buildarch: noarch
%if 0%{?fedora} && 0%{?fedora} > 26
BuildRequires:	perl-interpreter
%else
BuildRequires:	perl
%endif
BuildRequires:	perl(ExtUtils::MakeMaker)
%if 0%{?rhel} >= 7
BuildRequires:  perl(Data::Dumper)
%endif


Requires: %(perl -MConfig -le 'if (defined $Config{useithreads}) { print "perl(:WITH_ITHREADS)" } else { print "perl(:WITHOUT_ITHREADS)" }')
Requires: %(perl -MConfig -le 'if (defined $Config{usethreads}) { print "perl(:WITH_THREADS)" } else { print "perl(:WITHOUT_THREADS)" }')
Requires: %(perl -MConfig -le 'if (defined $Config{uselargefiles}) { print "perl(:WITH_LARGEFILES)" } else { print "perl(:WITHOUT_LARGEFILES)" }')
Source0: Mail-RFC822-Address-0.3.tar.gz

%description
Mail-RFC822-Address Perl module
%prep
%setup -q -n Mail-RFC822-Address-%{version}

%build
%if "%{perlgen}" == "5.8"
CFLAGS="$RPM_OPT_FLAGS" perl Makefile.PL PREFIX=$RPM_BUILD_ROOT%{_prefix} < /dev/null
%else
CFLAGS="$RPM_OPT_FLAGS" perl Makefile.PL < /dev/null
%endif

make
make test

%clean
rm -rf $RPM_BUILD_ROOT
%install

rm -rf $RPM_BUILD_ROOT
eval `perl '-V:installarchlib'`
mkdir -p $RPM_BUILD_ROOT/$installarchlib
%if "%{perlgen}" == "5.8"
make install
%else
make install PREFIX=$RPM_BUILD_ROOT%{_prefix}
%endif


[ -x /usr/lib/rpm/brp-compress ] && /usr/lib/rpm/brp-compress

rm -f `find $RPM_BUILD_ROOT -type f -name perllocal.pod -o -name .packlist`
find $RPM_BUILD_ROOT/usr -type f -print | \
	sed "s@^$RPM_BUILD_ROOT@@g" | \
	grep -v perllocal.pod | \
	grep -v "\.packlist" > Mail-RFC822-Address-%{version}-filelist
if [ "$(cat Mail-RFC822-Address-%{version}-filelist)X" = "X" ] ; then
    echo "ERROR: EMPTY FILE LIST"
    exit 1
fi

%files -f Mail-RFC822-Address-%{version}-filelist

%changelog
* Thu Aug 10 2017 Tomas Kasparek <tkasparek@redhat.com> 0.3-14
- 1479849 - BuildRequires: perl has been renamed to perl-interpreter on Fedora
  27

* Thu Mar 23 2017 Michael Mraka <michael.mraka@redhat.com> 0.3-13
- since Fedora 25 perl is not in standard buildroot

* Thu Jun 26 2014 Michael Mraka <michael.mraka@redhat.com> 0.3-12
- RHEL7 dependency

* Thu Jun 26 2014 Michael Mraka <michael.mraka@redhat.com> 0.3-11
- rebuild for RHEL7

* Thu Dec 12 2002 cturner@redhat.com
- Specfile autogenerated
